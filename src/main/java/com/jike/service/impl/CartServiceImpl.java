package com.jike.service.impl;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.jike.common.Const;
import com.jike.common.ResponseCode;
import com.jike.common.ServerResponse;
import com.jike.dao.CartMapper;
import com.jike.dao.ProductMapper;
import com.jike.pojo.Cart;
import com.jike.pojo.Product;
import com.jike.service.ICartService;
import com.jike.util.BigDecimalUtil;
import com.jike.util.PropertiesUtil;
import com.jike.vo.CartProductVo;
import com.jike.vo.CartVo;

@Service("iCartService")
public class CartServiceImpl implements ICartService {

	/**
	 * 购物车模块要理解Cart、Product、CartVo、CartProductVo之间的关系
	 * Cart、Product就是数据库对应的实体类
	 * CartVo 是自定义的类，包括购物车中的产品集合、购物车的总价、购物车的全选状态、图片四个元素
	 * CartProductVo 是购物车中展示的产品类，是Cart跟Product的结合体
	 * 
	 * 一定要能理解购物车的核心方法，所有对购物车的操作均基于这个核心方法  getCartVoLimit 返回的是一个CartVo对象
	 */

	@Resource
	private CartMapper cartMapper;

	@Resource
	ProductMapper productMapper;
	
	public ServerResponse<CartVo> list(Integer userId) {
		CartVo cartVo = this.getCartVoLimit(userId);
		return ServerResponse.createBySuccess(cartVo);
	}

	public ServerResponse<CartVo> add(Integer userId, Integer productId, Integer count) {
		if(productId == null || count == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
		Cart cart = cartMapper.selectCartByUserIdProductId(userId, productId);
		if (cart == null) {
			// 购物车中没有，往购物车中新增一条产品记录
			Cart cartItem = new Cart();
			cartItem.setUserId(userId);
			cartItem.setProductId(productId);
			cartItem.setQuantity(count);
			//产品第一次添加进购物车，默认为选中
			cartItem.setChecked(Const.Cart.CHECKED);
			cartMapper.insert(cartItem);
		} else {
			// 购物车中存在了，在数量上新增
			count = cart.getQuantity() + count;
			cart.setQuantity(count);
			cartMapper.updateByPrimaryKeySelective(cart);
		}
		return this.list(userId);
	}

	public ServerResponse<CartVo> update(Integer userId, Integer productId, Integer count) {
		if(productId == null || count == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Cart cart = cartMapper.selectCartByUserIdProductId(userId,productId);
        
        if(cart != null){
            cart.setQuantity(count);
        }
        cartMapper.updateByPrimaryKey(cart);
        return this.list(userId);
	}

	public ServerResponse<CartVo> deleteProduct(Integer userId, String productIds) {
		//因为存在删除多个购物车产品的情况，所有这里采用字符串将要删除的产品id拼接成字符串，到后台再进行拆分
		List<String> productList = Splitter.on(",").splitToList(productIds);
        if(CollectionUtils.isEmpty(productList)){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        cartMapper.deleteByUserIdProductIds(userId,productList);
        return this.list(userId);
	}



	public ServerResponse<CartVo> selectOrUnSelect(Integer userId, Integer productId, Integer checked) {
		cartMapper.checkedOrUncheckedProduct(userId,productId,checked);
        return this.list(userId);
	}

	public ServerResponse<Integer> getCartProductCount(Integer userId) {
		//userId为空，代表用户未登陆，不是报错，所以购物车数量显示为0即可
		if(userId == null){
            return ServerResponse.createBySuccess(0);
        }
        return ServerResponse.createBySuccess(cartMapper.selectCartProductCount(userId));
	}

	/**
	 * 购物车核心方法
	 */
	private CartVo getCartVoLimit(Integer userId) {
		CartVo cartVo = new CartVo();
		// 用户购物车中产品的集合
		List<CartProductVo> cartProductVoList = Lists.newArrayList();
		// 用户购物车里所有被选中产品的价格总和
		BigDecimal cartTotalPrice = new BigDecimal("0");// 这里一定要用Bigdecimal带String参数的构造器，以解决商业运算中浮点型丢失精度的问题
		// 1、获取用户的购物车集合
		List<Cart> cartList = cartMapper.selectCartByUserId(userId);
		// 2、如果有记录，就遍历，根据产品id拼装购物车产品信息（CartProductVo）
		if (CollectionUtils.isNotEmpty(cartList)) {
			CartProductVo cartProductVo = null;
			for (Cart cart : cartList) {
				cartProductVo = new CartProductVo();
				cartProductVo.setId(cart.getId());
				cartProductVo.setUserId(userId);
				cartProductVo.setProductId(cart.getProductId());

				// 根据购物车表中的产品ID从产品表中获取产品信息，用于拼装CartProductV
				Product product = productMapper.selectByPrimaryKey(cart.getProductId());
				// 进行非空判断
				if (product != null) {
					cartProductVo.setProductMainImage(product.getMainImage());
					cartProductVo.setProductName(product.getName());
					cartProductVo.setProductSubtitle(product.getSubtitle());
					cartProductVo.setProductStatus(product.getStatus());
					cartProductVo.setProductPrice(product.getPrice());
					cartProductVo.setProductStock(product.getStock());
					int buyLimitCount = 0;
					if (product.getStock() >= cart.getQuantity()) {
						// 产品的库存大于等于购物车里的数量，即为库存充足
						buyLimitCount = cart.getQuantity();
						cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_SUCCESS);
					} else {
						buyLimitCount = product.getStock();
						cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_FAIL);
						// 库存不足,要把购物车中这个产品的数量更新为最大库存量，不允许出现超出库存数量的情况
						Cart cartItem = new Cart();
						cartItem.setId(cart.getId());
						cartItem.setQuantity(product.getStock());
						cartMapper.updateByPrimaryKeySelective(cartItem);
					}
					cartProductVo.setQuantity(buyLimitCount);
					// 计算购物车中这个产品的总价
					cartProductVo.setProductTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(),
							cartProductVo.getQuantity().doubleValue()));
					// 选中状态
					cartProductVo.setProductChecked(cart.getChecked());
				}
				if (cart.getChecked() == Const.Cart.CHECKED) {
					// 如果是选中状态，就将价格纳入购物车总价格中
					cartTotalPrice = BigDecimalUtil.add(cartTotalPrice.doubleValue(), cartProductVo.getProductTotalPrice().doubleValue());
				}
				// 加入购物产品集合中
				cartProductVoList.add(cartProductVo);
			}
		}
		cartVo.setAllChecked(this.getAllCheckedStatus(userId));
		cartVo.setCartProductVoList(cartProductVoList);
		cartVo.setCartTotalPrice(cartTotalPrice);
		cartVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));
		return cartVo;
	}

	/**
	 * 判断用户是否全选
	 * 
	 * @param userId
	 * @return
	 */
	private boolean getAllCheckedStatus(Integer userId) {
		if (userId == null) {
			return false;
		}
		return cartMapper.selectCartProductCheckedStatusByUserId(userId) == 0;
	}

}
