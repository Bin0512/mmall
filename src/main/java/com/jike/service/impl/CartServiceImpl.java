package com.jike.service.impl;

import com.jike.common.ServerResponse;
import com.jike.service.ICartService;
import com.jike.vo.CartVo;

public class CartServiceImpl implements ICartService {

	public ServerResponse<CartVo> add(Integer userId, Integer productId, Integer count) {
		/**
         * 往购物车中添加商品的业务逻辑：
         * 1、根据用户ID和产品ID查询这个用户的购物车中是否有这个产品
         * 2、如果没有，就往购物车中新增一条产品记录
         * 3、如果有，就在原来的产品上数量增加
         */
		return null;
	}

	public ServerResponse<CartVo> update(Integer userId, Integer productId, Integer count) {
		
		
		
		
		return null;
	}

	public ServerResponse<CartVo> deleteProduct(Integer userId, String productIds) {
		
		return null;
	}

	public ServerResponse<CartVo> list(Integer userId) {
		
		return null;
	}

	public ServerResponse<CartVo> selectOrUnSelect(Integer userId, Integer productId, Integer checked) {
		
		return null;
	}

	public ServerResponse<Integer> getCartProductCount(Integer userId) {
		// TODO Auto-generated method stub
		return null;
	}

}
