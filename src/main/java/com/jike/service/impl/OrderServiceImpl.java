package com.jike.service.impl;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
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
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.jike.common.Const;
import com.jike.common.ServerResponse;
import com.jike.dao.CartMapper;
import com.jike.dao.OrderItemMapper;
import com.jike.dao.OrderMapper;
import com.jike.dao.PayInfoMapper;
import com.jike.dao.ProductMapper;
import com.jike.dao.ShippingMapper;
import com.jike.pojo.Cart;
import com.jike.pojo.Order;
import com.jike.pojo.OrderItem;
import com.jike.pojo.PayInfo;
import com.jike.pojo.Product;
import com.jike.pojo.Shipping;
import com.jike.service.IOrderService;
import com.jike.util.BigDecimalUtil;
import com.jike.util.DateTimeUtil;
import com.jike.util.FTPUtil;
import com.jike.util.PropertiesUtil;
import com.jike.vo.OrderItemVo;
import com.jike.vo.OrderProductVo;
import com.jike.vo.OrderVo;
import com.jike.vo.ShippingVo;

@Service("iOrderService")
public class OrderServiceImpl implements IOrderService {

	private Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

	@Resource
	private OrderMapper orderMapper;

	@Resource
	private OrderItemMapper orderItemMapper;

	@Resource
	private PayInfoMapper payInfoMapper;
	
	@Resource
	private CartMapper cartMapper;
    
	@Resource
    private ProductMapper productMapper;
	
    @Resource
    private ShippingMapper shippingMapper;

	// 支付宝当面付2.0服务
	private static AlipayTradeService tradeService;

	static {
		/**
		 * 一定要在创建AlipayTradeService之前调用Configs.init()设置默认参数
		 * Configs会读取classpath下的zfbinfo.properties文件配置信息，如果找不到该文件则确认该文件是否在classpath目录
		 */
		Configs.init("zfbinfo.properties");

		/**
		 * 使用Configs提供的默认参数 AlipayTradeService可以使用单例或者为静态成员对象，不需要反复new
		 */
		tradeService = new AlipayTradeServiceImpl.ClientBuilder().build();
	}

	@Override
	public ServerResponse pay(Long orderNo, Integer userId, String path) {
		// 创建一个Map集合，用于存储返回给前端的订单号和二维码
		Map<String, String> map = new HashMap<>();
		// 根据用户ID和订单号校验该订单是否属于该用户，避免横向越权漏洞
		Order order = orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
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
		String body = new StringBuilder().append("订单").append(outTradeNo).append("购买商品共").append(totalAmount)
				.append("元").toString();

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

		// 根据用户ID和订单编号，获取订单详情信息，通过订单详情信息来填充商品详情GoodsDetail,再加入到商品详情列表中
		List<OrderItem> orderItems = orderItemMapper.getByOrderNoUserId(orderNo, userId);

		for (OrderItem orderItem : orderItems) {
			// 创建一个商品信息，参数含义分别为商品id（使用国标）、名称、单价（单位为分）、数量，如果需要添加商品类别，详见GoodsDetail
			GoodsDetail goodsDetail = GoodsDetail.newInstance(orderItem.getProductId().toString(),
					orderItem.getProductName(),
					BigDecimalUtil.mul(orderItem.getCurrentUnitPrice().doubleValue(), new Double(100).doubleValue())
							.longValue(),
					orderItem.getQuantity());
			// 创建好一个商品后添加至商品明细列表
			goodsDetailList.add(goodsDetail);
		}

		// 创建扫码支付请求builder，设置请求参数
		AlipayTradePrecreateRequestBuilder builder = new AlipayTradePrecreateRequestBuilder().setSubject(subject)
				.setTotalAmount(totalAmount).setOutTradeNo(outTradeNo).setUndiscountableAmount(undiscountableAmount)
				.setSellerId(sellerId).setBody(body).setOperatorId(operatorId).setStoreId(storeId)
				.setExtendParams(extendParams).setTimeoutExpress(timeoutExpress)
				.setNotifyUrl(PropertiesUtil.getProperty("alipay.callback.url"))// 支付宝服务器主动通知商户服务器里指定的页面http路径,根据需要设置
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
			// 二维码路径
			String qrPath = String.format(path + "/qr-%s.png", response.getOutTradeNo());
			String qrFileName = String.format("qr-%s.png", response.getOutTradeNo());
			// 生成二维码图片
			ZxingUtils.getQRCodeImge(response.getQrCode(), 256, qrPath);

			// 将二维码上传到FTP服务器中
			File targetFile = new File(path, qrFileName);
			try {
				FTPUtil.uploadFile(Lists.newArrayList(targetFile));
			} catch (IOException e) {
				log.error("上传二维码异常", e);
			}

			// 获取ftp服务器上的二维码地址，传给前端
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
				log.info(String.format("subCode:%s, subMsg:%s", response.getSubCode(), response.getSubMsg()));
			}
			log.info("body:" + response.getBody());
		}
	}

	@Override
	public ServerResponse aliCallback(Map<String, String> params) {
		// 商户订单号
		Long orderNo = Long.parseLong(params.get("out_trade_no"));
		// 支付宝交易号
		String tradeNo = params.get("trade_no");
		// 交易状态
		String tradeStatus = params.get("trade_status");

		// 验证订单的正确性，看是不是这个商户的订单
		Order order = orderMapper.selectByOrderNo(orderNo);
		if (order == null) {
			return ServerResponse.createByErrorMessage("非快乐慕商城的订单,回调忽略");
		}
		// 验证这个订单是否已经被支付
		if (order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()) {
			return ServerResponse.createBySuccess("支付宝重复调用");
		}
		// 验证是否交易成功，此处的交易是指跟支付宝的
		if (Const.AlipayCallback.TRADE_STATUS_TRADE_SUCCESS.equals(tradeStatus)) {
			// gmt_payment为交易付款时间，支付宝返回。
			order.setPaymentTime(DateTimeUtil.strToDate(params.get("gmt_payment")));
			// 设置订单为已支付状态
			order.setStatus(Const.OrderStatusEnum.PAID.getCode());
			// 更新到数据库
			orderMapper.updateByPrimaryKeySelective(order);
		}

		// 将这次支付信息传入数据库
		PayInfo payInfo = new PayInfo();
		payInfo.setUserId(order.getUserId());
		payInfo.setOrderNo(order.getOrderNo());
		payInfo.setPayPlatform(Const.PayPlatformEnum.ALIPAY.getCode());// 为了易于扩展，此处使用枚举，以后会引入微信支付
		payInfo.setPlatformNumber(tradeNo);
		payInfo.setPlatformStatus(tradeStatus);

		payInfoMapper.insert(payInfo);

		return ServerResponse.createBySuccess();
	}

	@Override
	public ServerResponse queryOrderPayStatus(Integer userId, Long orderNo) {
		Order order = orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
		if (order == null) {
			return ServerResponse.createByErrorMessage("用户没有该订单");
		}
		if (order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()) {
			return ServerResponse.createBySuccess();
		}
		return ServerResponse.createByError();
	}

	// 创建订单开始
	/**
	 * 创建订单 1.1、根据用户ID从购物车中获取被选中的购物车列表 1.2、通过购物车中的产品信息，生成订单详情记录，此时的订单详情记录还缺少订单号
	 * 1.3、通过订单详情，得到订单的总价格 1.4、通过计算出的总价格，传入收货地址id和用户id生成订单 1.5、为每一个订单详情设置订单号
	 * 1.6、对完全体的订单详情进行持久化操作，存入数据库。技术点：mybatis批量插入
	 * 1.7、通过订单详情中用户购买的数量，对数据库中的产品库存做相应的减少。 1.8、请空购物车中被选中的产品，及时更新购物车信息
	 * 1.9、组装订单创建成功后前端需要返回的订单信息
	 */
	public ServerResponse createOrder(Integer userId, Integer shippingId) {
		// 1、根据用户ID从购物车中获取被选中的购物车列表
		List<Cart> cartList = cartMapper.selectCheckedCartByUserId(userId);

		// 2、通过购物车中的产品信息，生成订单详情记录，此时的订单详情记录还缺少订单号
		ServerResponse serverResponse = this.getCartOrderItem(userId, cartList);
		if (!serverResponse.isSuccess()) {
			return serverResponse;
		}
		List<OrderItem> orderItemList = (List<OrderItem>) serverResponse.getData();

		// 3、通过订单详情，得到订单的总价格
		if (CollectionUtils.isEmpty(orderItemList)) {
			return ServerResponse.createByErrorMessage("购物车为空");
		}
		BigDecimal payment = this.getOrderTotalPrice(orderItemList);

		// 4、通过计算出的总价格，传入收货地址id和用户id生成订单
		Order order = this.assembleOrder(userId, shippingId, payment);
		if (order == null) {
			return ServerResponse.createByErrorMessage("生成订单错误");
		}

		// 5、为每一个订单详情设置订单号
		for (OrderItem orderItem : orderItemList) {
			orderItem.setOrderNo(order.getOrderNo());
		}

		// 6、对完全体的订单详情进行持久化操作，存入数据库。技术点：mybatis批量插入
		orderItemMapper.batchInsert(orderItemList);

		// 7、通过订单详情中用户购买的数量，对数据库中的产品库存做相应的减少。
		this.reduceProductStock(orderItemList);

		// 8、请空购物车中被选中的产品
		this.cleanCart(cartList);

		// 9、组装前端需要的订单信息
		OrderVo orderVo = assembleOrderVo(order, orderItemList);
		return ServerResponse.createBySuccess(orderVo);
	}

	/**
	 * 通过遍历购物车中的产品信息，为每个选中的产品生成一条订单详情记录 涉及到三张表：Cart表、Product表、OrderItem表
	 * 关系是：Cart表中存储了productId字段，可以通过这个字段从Product表中获取到产品信息，并填充到OrderItem表中去
	 * 将产品填充到OrderItem表中的前置条件是： 产品必须是得是在售状态、用户购物车中选择的产品数量不能大于产品的库存
	 * 
	 * @param userId
	 * @param cartList
	 * @return
	 */
	private ServerResponse getCartOrderItem(Integer userId, List<Cart> cartList) {
		List<OrderItem> orderItemList = Lists.newArrayList();
		if (CollectionUtils.isEmpty(cartList)) {
			return ServerResponse.createByErrorMessage("购物车为空");
		}

		// 校验购物车的数据,包括产品的状态和数量
		for (Cart cartItem : cartList) {
			OrderItem orderItem = new OrderItem();
			Product product = productMapper.selectByPrimaryKey(cartItem.getProductId());
			// 判断是否为在售状态
			if (Const.ProductStatusEnum.ON_SALE.getCode() != product.getStatus()) {
				return ServerResponse.createByErrorMessage("产品" + product.getName() + "不是在线售卖状态");
			}

			// 校验库存
			if (cartItem.getQuantity() > product.getStock()) {
				return ServerResponse.createByErrorMessage("产品" + product.getName() + "库存不足");
			}

			orderItem.setUserId(userId);
			orderItem.setProductId(product.getId());
			orderItem.setProductName(product.getName());
			orderItem.setProductImage(product.getMainImage());
			// 当前购买时的单价
			orderItem.setCurrentUnitPrice(product.getPrice());
			orderItem.setQuantity(cartItem.getQuantity());
			orderItem.setTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(), cartItem.getQuantity()));
			orderItemList.add(orderItem);
		}
		return ServerResponse.createBySuccess(orderItemList);
	}

	/**
	 * 通过订单详情，得到订单的总价格
	 * 
	 * @param orderItemList
	 * @return
	 */
	private BigDecimal getOrderTotalPrice(List<OrderItem> orderItemList) {
		BigDecimal payment = new BigDecimal("0");
		for (OrderItem orderItem : orderItemList) {
			payment = BigDecimalUtil.add(payment.doubleValue(), orderItem.getTotalPrice().doubleValue());
		}
		return payment;
	}

	/**
	 * 生成订单
	 * 
	 * @param userId
	 * @param shippingId
	 * @param payment
	 * @return
	 */
	private Order assembleOrder(Integer userId, Integer shippingId, BigDecimal payment) {
		Order order = new Order();
		long orderNo = this.generateOrderNo();
		order.setOrderNo(orderNo);
		order.setStatus(Const.OrderStatusEnum.NO_PAY.getCode());
		order.setPostage(0);
		order.setPaymentType(Const.PaymentTypeEnum.ONLINE_PAY.getCode());
		order.setPayment(payment);

		order.setUserId(userId);
		order.setShippingId(shippingId);
		// 发货时间等等
		// 付款时间等等
		int rowCount = orderMapper.insert(order);
		if (rowCount > 0) {
			return order;
		}
		return null;
	}

	private long generateOrderNo() {
		// 订单号生成规则

		long currentTime = System.currentTimeMillis();
		// 生成0-99随机数，加入随机数，可以避免由于高并发带来的订单号相同而报错的情况
		return currentTime + new Random().nextInt(100);
	}

	/**
	 * 减少多个产品的库存
	 * 
	 * @param orderItemList
	 */
	private void reduceProductStock(List<OrderItem> orderItemList) {
		for (OrderItem orderItem : orderItemList) {
			Product product = productMapper.selectByPrimaryKey(orderItem.getProductId());
			product.setStock(product.getStock() - orderItem.getQuantity());
			productMapper.updateByPrimaryKeySelective(product);
		}
	}

	/**
	 * 清空购物车中被选中的产品
	 * 
	 * @param cartList
	 */
	private void cleanCart(List<Cart> cartList) {
		for (Cart cart : cartList) {
			cartMapper.deleteByPrimaryKey(cart.getId());
		}
	}

	/**
	 * 组装订单信息
	 * 
	 * @param order
	 * @param orderItemList
	 * @return
	 */
	private OrderVo assembleOrderVo(Order order, List<OrderItem> orderItemList) {
		OrderVo orderVo = new OrderVo();
		orderVo.setOrderNo(order.getOrderNo());
		orderVo.setPayment(order.getPayment());
		orderVo.setPaymentType(order.getPaymentType());
		orderVo.setPaymentTypeDesc(Const.PaymentTypeEnum.codeOf(order.getPaymentType()).getValue());

		orderVo.setPostage(order.getPostage());
		orderVo.setStatus(order.getStatus());
		orderVo.setStatusDesc(Const.OrderStatusEnum.codeOf(order.getStatus()).getValue());

		orderVo.setShippingId(order.getShippingId());
		Shipping shipping = shippingMapper.selectByPrimaryKey(order.getShippingId());
		if (shipping != null) {
			orderVo.setReceiverName(shipping.getReceiverName());
			orderVo.setShippingVo(assembleShippingVo(shipping));
		}

		orderVo.setPaymentTime(DateTimeUtil.dateToStr(order.getPaymentTime()));
		orderVo.setSendTime(DateTimeUtil.dateToStr(order.getSendTime()));
		orderVo.setEndTime(DateTimeUtil.dateToStr(order.getEndTime()));
		orderVo.setCreateTime(DateTimeUtil.dateToStr(order.getCreateTime()));
		orderVo.setCloseTime(DateTimeUtil.dateToStr(order.getCloseTime()));

		orderVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));

		List<OrderItemVo> orderItemVoList = Lists.newArrayList();

		for (OrderItem orderItem : orderItemList) {
			OrderItemVo orderItemVo = assembleOrderItemVo(orderItem);
			orderItemVoList.add(orderItemVo);
		}
		orderVo.setOrderItemVoList(orderItemVoList);
		return orderVo;
	}
	
	/**
	 * 组装前端用于展示的收货地址信息，即：ShippingVo
	 * 
	 * @param shipping
	 * @return
	 */
	private ShippingVo assembleShippingVo(Shipping shipping) {
		ShippingVo shippingVo = new ShippingVo();
		shippingVo.setReceiverName(shipping.getReceiverName());
		shippingVo.setReceiverAddress(shipping.getReceiverAddress());
		shippingVo.setReceiverProvince(shipping.getReceiverProvince());
		shippingVo.setReceiverCity(shipping.getReceiverCity());
		shippingVo.setReceiverDistrict(shipping.getReceiverDistrict());
		shippingVo.setReceiverMobile(shipping.getReceiverMobile());
		shippingVo.setReceiverZip(shipping.getReceiverZip());
		shippingVo.setReceiverPhone(shippingVo.getReceiverPhone());
		return shippingVo;
	}
	
    /**
     * 组装OrderItemVo
     * @param orderItem
     * @return
     */
    private OrderItemVo assembleOrderItemVo(OrderItem orderItem){
        OrderItemVo orderItemVo = new OrderItemVo();
        orderItemVo.setOrderNo(orderItem.getOrderNo());
        orderItemVo.setProductId(orderItem.getProductId());
        orderItemVo.setProductName(orderItem.getProductName());
        orderItemVo.setProductImage(orderItem.getProductImage());
        orderItemVo.setCurrentUnitPrice(orderItem.getCurrentUnitPrice());
        orderItemVo.setQuantity(orderItem.getQuantity());
        orderItemVo.setTotalPrice(orderItem.getTotalPrice());

        orderItemVo.setCreateTime(DateTimeUtil.dateToStr(orderItem.getCreateTime()));
        return orderItemVo;
    }
   
	// 创建订单结束

    /**
     * 获取订单商品信息
     * 2.1、根据用户ID从购物车中获取被选中的购物车列表
     * 2.2、通过购物车中的产品信息，生成订单详情记录
     * 2.3、通过订单详情记录组装前端需要展示的订单详情数据，即：OrderItemVo,并计算出所有订单详情的总价，前端要用于展示。
     * 2.4、组装前端需要的订单商品信息，即：OrderProductVo
     * 	
     */
    public ServerResponse getOrderCartProduct(Integer userId){
        //1、根据用户ID从购物车中获取被选中的购物车列表
        List<Cart> cartList = cartMapper.selectCheckedCartByUserId(userId);
        
        //2、通过购物车中的产品信息，生成订单详情记录
        ServerResponse serverResponse =  this.getCartOrderItem(userId,cartList);
        if(!serverResponse.isSuccess()){
            return serverResponse;
        }
        List<OrderItem> orderItemList =( List<OrderItem> ) serverResponse.getData();

        //3、通过订单详情记录组装前端需要展示的订单详情数据，即：OrderItemVo,并计算出所有订单详情的总价，前端要用于展示。
        List<OrderItemVo> orderItemVoList = Lists.newArrayList();
        BigDecimal payment = new BigDecimal("0");
        for(OrderItem orderItem : orderItemList){
            payment = BigDecimalUtil.add(payment.doubleValue(),orderItem.getTotalPrice().doubleValue());
            orderItemVoList.add(assembleOrderItemVo(orderItem));
        }
        
        //4、组装前端需要的订单商品信息，即：OrderProductVo,
        OrderProductVo orderProductVo = new OrderProductVo();
        orderProductVo.setProductTotalPrice(payment);
        orderProductVo.setOrderItemVoList(orderItemVoList);
        orderProductVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));
        return ServerResponse.createBySuccess(orderProductVo);
    }
   
    /**
     * 获取订单列表，并分页显示
     * 3.1、通过用户ID从数据库中获取该用户所有的订单信息，即：Order
     * 3.2、通过从数据库获得的订单信息组装前端需要展示的订单信息数据，即：OrderVo,等同于在创建订单成功后，返回的订单数据信息
     * 3.3、设置分页信息
     */
    public ServerResponse<PageInfo> getOrderList(Integer userId,int pageNum,int pageSize){
    	//页面初始化
        PageHelper.startPage(pageNum,pageSize);
        
    	List<Order> orderList = orderMapper.selectByUserId(userId);
        List<OrderVo> orderVoList = assembleOrderVoList(orderList,userId);
    	
        //通过从数据库获取的集合来初始化分页信息，比如：总记录数，思考一下，为什么这里不填orderVoList
        PageInfo pageResult = new PageInfo(orderList);
        //真正用于展示的结果集
        pageResult.setList(orderVoList);
        return ServerResponse.createBySuccess(pageResult);
    }

    /**
     * 获取OrderVoList
     * 分管理员和普通用户，复用此方法，根据用户ID是否为空来判断
     * @param orderList
     * @param userId
     * @return
     */
    private List<OrderVo> assembleOrderVoList(List<Order> orderList,Integer userId){
        List<OrderVo> orderVoList = Lists.newArrayList();
        for(Order order : orderList){
            List<OrderItem>  orderItemList = Lists.newArrayList();
            if(userId == null){
                //todo 管理员查询的时候 不需要传userId
                orderItemList = orderItemMapper.getByOrderNo(order.getOrderNo());
            }else{
                orderItemList = orderItemMapper.getByOrderNoUserId(order.getOrderNo(),userId);
            }
            OrderVo orderVo = assembleOrderVo(order,orderItemList);
            orderVoList.add(orderVo);
        }
        return orderVoList;
    }
    
    /**
     * 获取订单详情
     *4.1、根据用户ID和订单编号获取订单信息
     *4.2、非空判断，避免横向越权漏洞
     *4.3、通过用户ID和订单编号获取订单详情信息
     *4.4、组装前端需要展示的订单信息，即：OrderVo
     */
    public ServerResponse<OrderVo> getOrderDetail(Integer userId,Long orderNo){
    	//4.1、根据用户ID和订单编号获取订单信息
        Order order = orderMapper.selectByUserIdAndOrderNo(userId,orderNo);
        //4.2、非空判断，避免横向越权漏洞
        if(order != null){
        	//4.3、通过用户ID和订单编号获取订单详情信息
            List<OrderItem> orderItemList = orderItemMapper.getByOrderNoUserId(orderNo,userId);
            //4.4、组装前端需要展示的订单信息，即：OrderVo
            OrderVo orderVo = assembleOrderVo(order,orderItemList);
            return ServerResponse.createBySuccess(orderVo);
        }
        return  ServerResponse.createByErrorMessage("没有找到该订单");
    }

    /**
     * 取消订单
     *5.1、根据用户ID和订单编号获取订单信息
     *5.2、对获得的订单信息做非空判断，避免横向越权漏洞
     *5.3、判断这个订单的状态是不是已付款，已付款的订单不能再取消
     *5.4、将订单状态设为已取消，并持久化到数据库
     */
    public ServerResponse<String> cancel(Integer userId,Long orderNo){
        Order order  = orderMapper.selectByUserIdAndOrderNo(userId,orderNo);
        if(order == null){
            return ServerResponse.createByErrorMessage("该用户此订单不存在");
        }
        if(order.getStatus() != Const.OrderStatusEnum.NO_PAY.getCode()){
            return ServerResponse.createByErrorMessage("已付款,无法取消订单");
        }
        Order updateOrder = new Order();
        updateOrder.setId(order.getId());
        updateOrder.setStatus(Const.OrderStatusEnum.CANCELED.getCode());

        int row = orderMapper.updateByPrimaryKeySelective(updateOrder);
        if(row > 0){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }


    //backend
    /**
     * 后台订单列表
     * 与前台类似，只是没有传UserId
     */
    public ServerResponse<PageInfo> manageList(int pageNum,int pageSize){
        PageHelper.startPage(pageNum,pageSize);
        List<Order> orderList = orderMapper.selectAllOrder();
        List<OrderVo> orderVoList = this.assembleOrderVoList(orderList,null);
        PageInfo pageResult = new PageInfo(orderList);
        pageResult.setList(orderVoList);
        return ServerResponse.createBySuccess(pageResult);
    }

    /**
     * 后台订单详情
     */
    public ServerResponse<OrderVo> manageDetail(Long orderNo){
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order != null){
            List<OrderItem> orderItemList = orderItemMapper.getByOrderNo(orderNo);
            OrderVo orderVo = assembleOrderVo(order,orderItemList);
            return ServerResponse.createBySuccess(orderVo);
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }

    /**
     * 后台搜索，根据订单号进行查询
     */
    public ServerResponse<PageInfo> manageSearch(Long orderNo,int pageNum,int pageSize){
        PageHelper.startPage(pageNum,pageSize);
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order != null){
            List<OrderItem> orderItemList = orderItemMapper.getByOrderNo(orderNo);
            OrderVo orderVo = assembleOrderVo(order,orderItemList);

            PageInfo pageResult = new PageInfo(Lists.newArrayList(order));
            pageResult.setList(Lists.newArrayList(orderVo));
            return ServerResponse.createBySuccess(pageResult);
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }

    /**
     * 后台订单发货
     * 仅对订单状态是已支付的才可以发货
     * 
     */
    public ServerResponse<String> manageSendGoods(Long orderNo){
        Order order= orderMapper.selectByOrderNo(orderNo);
        if(order != null){
            if(order.getStatus() == Const.OrderStatusEnum.PAID.getCode()){
                order.setStatus(Const.OrderStatusEnum.SHIPPED.getCode());
                order.setSendTime(new Date());
                orderMapper.updateByPrimaryKeySelective(order);
                return ServerResponse.createBySuccess("发货成功");
            }
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }

	@Override
	public void closeOrder(Integer hour) {
		//1、获取所有超过规定时间且没有支付的订单
		Date closeDateTime = DateUtils.addHours(new Date(), -hour);
		List<Order> orderList = orderMapper.selectOrderStatusByCreateTime(Const.OrderStatusEnum.NO_PAY.getCode(), DateTimeUtil.dateToStr(closeDateTime));
		for (Order order : orderList) {
			List<OrderItem> orderItems = orderItemMapper.getByOrderNo(order.getOrderNo());
			for (OrderItem orderItem : orderItems) {
				Integer stock = productMapper.selectStockByProductId(orderItem.getProductId());
				//2、更新库存，如果在关闭订单之前，该商品已经被删除了，就不必更新数量了
				if (stock == null) {
					continue;
				}
				Product product = new Product();
				product.setId(orderItem.getProductId());
				product.setStock(stock + orderItem.getQuantity());
				productMapper.updateByPrimaryKeySelective(product);
			}
			//3、更新订单状态
			orderMapper.closeOrderByOrderId(order.getId());
			log.info("关闭订单OrderNo：{}",order.getOrderNo());
		}
	}



}
