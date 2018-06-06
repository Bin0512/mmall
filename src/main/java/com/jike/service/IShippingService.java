package com.jike.service;

import com.github.pagehelper.PageInfo;
import com.jike.common.ServerResponse;
import com.jike.pojo.Shipping;

public interface IShippingService {
	/**
	 * 增加地址信息
	 * @param userId
	 * @param shipping
	 * @return
	 */
	ServerResponse add(Integer userId, Shipping shipping);
	
	/**
	 * 删除地址信息
	 * @param userId
	 * @param shippingId
	 * @return
	 */
    ServerResponse<String> del(Integer userId,Integer shippingId);
    
    /**
     * 更新地址信息
     * @param userId
     * @param shipping
     * @return
     */
    ServerResponse update(Integer userId, Shipping shipping);
    
    /**
     * 查询地址信息
     * @param userId
     * @param shippingId
     * @return
     */
    ServerResponse<Shipping> select(Integer userId, Integer shippingId);
    
    /**
     * 地址列表，分页显示
     * @param userId
     * @param pageNum
     * @param pageSize
     * @return
     */
    ServerResponse<PageInfo> list(Integer userId, int pageNum, int pageSize);
}

