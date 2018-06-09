package com.jike.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alipay.api.AlipayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.GoodsDetail;
import com.alipay.demo.trade.model.builder.AlipayTradePrecreateRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.service.impl.AlipayTradeServiceImpl;
import com.alipay.demo.trade.utils.ZxingUtils;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.jike.common.ServerResponse;
import com.jike.dao.OrderItemMapper;
import com.jike.dao.OrderMapper;
import com.jike.pojo.Order;
import com.jike.pojo.OrderItem;
import com.jike.service.IOrderService;
import com.jike.util.BigDecimalUtil;
import com.jike.util.FTPUtil;
import com.jike.util.PropertiesUtil;
import com.jike.vo.OrderVo;


@Service("iOrderService")
public class OrderServiceImpl implements IOrderService {
	
	private Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

	@Resource
	private OrderMapper orderMapper;
	
	@Resource
	private OrderItemMapper orderItemMapper;
	
    // 支付宝当面付2.0服务
    private static AlipayTradeService   tradeService;
    
    static {
        /** 一定要在创建AlipayTradeService之前调用Configs.init()设置默认参数
         *  Configs会读取classpath下的zfbinfo.properties文件配置信息，如果找不到该文件则确认该文件是否在classpath目录
         */
        Configs.init("zfbinfo.properties");

        /** 使用Configs提供的默认参数
         *  AlipayTradeService可以使用单例或者为静态成员对象，不需要反复new
         */
        tradeService = new AlipayTradeServiceImpl.ClientBuilder().build();
    }

	@Override
	public ServerResponse pay(Long orderNo, Integer userId, String path) {
		// 创建一个Map集合，用于存储返回给前端的订单号和二维码
		Map<String, String> map = new HashMap<>();
		// 根据用户ID和订单号校验该订单是否属于该用户，避免横向越权漏洞
		Order order = orderMapper.selectByOrderNoAndUserId(orderNo, userId);
		if (order == null) {
			return ServerResponse.createByErrorMessage("该用户没有这个订单");
		}
		// 确实存在，就存入集合中
		map.put("orderNo", order.getOrderNo().toString());

		// (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
		// 需保证商户系统端不能重复，建议通过数据库sequence生成，
		String outTradeNo = order.getOrderNo().toString();

		// (必填) 订单标题，粗略描述用户的支付目的。如“xxx品牌xxx门店当面付扫码消费”
		String subject = new StringBuilder().append("happymmall扫码支付,订单号:").append(outTradeNo).toString();

		// (必填) 订单总金额，单位为元，不能超过1亿元
		// 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
		String totalAmount = order.getPayment().toString();

		// (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
		// 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
		String undiscountableAmount = "";

		// 卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
		// 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
		String sellerId = "";

		// 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"
		String body = new StringBuilder().append("订单").append(outTradeNo).append("购买商品共").append(totalAmount).append("元").toString();

		// 商户操作员编号，添加此参数可以为商户操作员做销售统计
		String operatorId = "test_operator_id";

		// (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
		String storeId = "test_store_id";

		// 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
		ExtendParams extendParams = new ExtendParams();
		extendParams.setSysServiceProviderId("2088100200300400500");

		// 支付超时，定义为120分钟
		String timeoutExpress = "120m";

		// 商品明细列表，需填写购买商品详细信息，
		List<GoodsDetail> goodsDetailList = new ArrayList<GoodsDetail>();
		
		//根据用户ID和订单编号，获取订单详情信息，通过订单详情信息来填充商品详情GoodsDetail,再加入到商品详情列表中
		List<OrderItem> orderItems = orderItemMapper.getByOrderNoUserId(orderNo,userId);
		
		for (OrderItem orderItem : orderItems) {
			// 创建一个商品信息，参数含义分别为商品id（使用国标）、名称、单价（单位为分）、数量，如果需要添加商品类别，详见GoodsDetail
			GoodsDetail goodsDetail = GoodsDetail.newInstance(orderItem.getProductId().toString(), orderItem.getProductName(),
										BigDecimalUtil.mul(orderItem.getCurrentUnitPrice().doubleValue(), new Double(100).doubleValue()).longValue(), orderItem.getQuantity());
			// 创建好一个商品后添加至商品明细列表
			goodsDetailList.add(goodsDetail);
		}
		
		// 创建扫码支付请求builder，设置请求参数
		AlipayTradePrecreateRequestBuilder builder = new AlipayTradePrecreateRequestBuilder().setSubject(subject)
				.setTotalAmount(totalAmount).setOutTradeNo(outTradeNo).setUndiscountableAmount(undiscountableAmount)
				.setSellerId(sellerId).setBody(body).setOperatorId(operatorId).setStoreId(storeId)
				.setExtendParams(extendParams).setTimeoutExpress(timeoutExpress)
				.setNotifyUrl(PropertiesUtil.getProperty("alipay.callback.url"))//支付宝服务器主动通知商户服务器里指定的页面http路径,根据需要设置
				.setGoodsDetailList(goodsDetailList);

		AlipayF2FPrecreateResult result = tradeService.tradePrecreate(builder);
		switch (result.getTradeStatus()) {
		case SUCCESS:
			log.info("支付宝预下单成功: )");

			AlipayTradePrecreateResponse response = result.getResponse();
			dumpResponse(response);
			
			File folder = new File(path);
			if (!folder.exists()) {
				folder.setWritable(true);
				folder.mkdirs();
			}

			// 需要修改为运行机器上的路径
			//二维码路径
			String qrPath = String.format(path + "/qr-%s.png", response.getOutTradeNo());
			String qrFileName = String.format("qr-%s.png",response.getOutTradeNo());
			//生成二维码图片
			ZxingUtils.getQRCodeImge(response.getQrCode(), 256, qrPath);
			
			//将二维码上传到FTP服务器中
			File targetFile = new File(path,qrFileName);
			try {
				FTPUtil.uploadFile(Lists.newArrayList(targetFile));
			} catch (IOException e) {
				log.error("上传二维码异常",e);
			}
			
			//获取ftp服务器上的二维码地址，传给前端
			String qrUrl = PropertiesUtil.getProperty("ftp.server.http.prefix") + targetFile.getName();
			map.put("quUrl", qrUrl);
			return ServerResponse.createBySuccess(map);
		case FAILED:
			log.error("支付宝预下单失败!!!");
			return ServerResponse.createByErrorMessage("支付宝预下单失败!!!");

		case UNKNOWN:
			log.error("系统异常，预下单状态未知!!!");
			return ServerResponse.createByErrorMessage("系统异常，预下单状态未知!!!");

		default:
			log.error("不支持的交易状态，交易返回异常!!!");
			return ServerResponse.createByErrorMessage("不支持的交易状态，交易返回异常!!!");
		}
	}

	 // 简单打印应答,直接从支付宝官方接口里的考不
    private void dumpResponse(AlipayResponse response) {
        if (response != null) {
            log.info(String.format("code:%s, msg:%s", response.getCode(), response.getMsg()));
            if (StringUtils.isNotEmpty(response.getSubCode())) {
                log.info(String.format("subCode:%s, subMsg:%s", response.getSubCode(),
                    response.getSubMsg()));
            }
            log.info("body:" + response.getBody());
        }
    }
	
	@Override
	public ServerResponse aliCallback(Map<String, String> params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServerResponse queryOrderPayStatus(Integer userId, Long orderNo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServerResponse createOrder(Integer userId, Integer shippingId) {
		/**
    	 * 创建订单的业务逻辑：
    	 * 1、从购物车中获取被选中的产品
    	 * 2、计算这个订单的总价
    	 * 		2.1在计算前先判断购物车里是否有产品
    	 */
		/**
         * 通过购物车列表和用户ID获取订单详情信息列表，分以下几步：
         * 		首先，通过购物车中的产品Id从产品表中获取产品的信息，
         * 		然后，校验产品的在售状态和库存。
         * 		最后，根据产品组装订单信息
         * 
         * 通过遍历每一辆购物车，可获得购物车中的产品ID，再通过这个产品ID从产品表中拿到产品的信息，
         * 由此可以判断产品是否是在售状态，同时通过购物车里的产品数量与产品表中的产品库存进行比较，判断库存是否充足
         * 在产品处于在售状态且库存充足的情况下，就可以根据产品信息生成一个订单详情记录
         * 将该记录放入集合返回
         * 每一条订单详情都会有一个价格（即该产品的单价*用户购买该产品的数量）
         * 通过对每一条订单详情价格的汇总从而得出最终的总价
         */
		return null;
	}

	@Override
	public ServerResponse<String> cancel(Integer userId, Long orderNo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServerResponse getOrderCartProduct(Integer userId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServerResponse<OrderVo> getOrderDetail(Integer userId, Long orderNo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServerResponse<PageInfo> getOrderList(Integer userId, int pageNum, int pageSize) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServerResponse<PageInfo> manageList(int pageNum, int pageSize) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServerResponse<OrderVo> manageDetail(Long orderNo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServerResponse<PageInfo> manageSearch(Long orderNo, int pageNum, int pageSize) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServerResponse<String> manageSendGoods(Long orderNo) {
		// TODO Auto-generated method stub
		return null;
	}

}
