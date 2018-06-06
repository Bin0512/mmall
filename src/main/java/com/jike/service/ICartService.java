package com.jike.service;

import com.jike.common.ServerResponse;
import com.jike.vo.CartVo;


public interface ICartService {
	
    /**
     * 查询购物车中的所有产品
     * @param userId
     * @return
     */
    ServerResponse<CartVo> list (Integer userId);
	
	/**
	 * 往购物车中添加商品
	 * @param userId
	 * @param productId
	 * @param count
	 * @return
	 */
    ServerResponse<CartVo> add(Integer userId, Integer productId, Integer count);
    
    /**
     * 更新购物车中产品的数量
     * @param userId
     * @param productId
     * @param count
     * @return
     */
    ServerResponse<CartVo> update(Integer userId,Integer productId,Integer count);
    
    /**
     * 删除购物中的产品
     * @param userId
     * @param productIds
     * @return
     */
    ServerResponse<CartVo> deleteProduct(Integer userId,String productIds);
    
    
    /**
     * 全选或者全反选
     * @param userId
     * @param productId
     * @param checked
     * @return
     */
    ServerResponse<CartVo> selectOrUnSelect (Integer userId,Integer productId,Integer checked);

    /**
     * 获取购物车中所有产品的数量
     * @param userId
     * @return
     */
    ServerResponse<Integer> getCartProductCount(Integer userId);
}
