package com.jike.service;

import java.util.Map;

import com.github.pagehelper.PageInfo;
import com.jike.common.ServerResponse;
import com.jike.vo.OrderVo;

public interface IOrderService {
	/**
	 * 支付宝支付
	 * @param orderNo
	 * @param userId
	 * @param path
	 * @return
	 */
    ServerResponse pay(Long orderNo, Integer userId, String path);
    
    /**
     * 支付宝回调
     * @param params
     * @return
     */
    ServerResponse aliCallback(Map<String,String> params);
    
    /**
     * 查询订单的支付状态
     * @param userId
     * @param orderNo
     * @return
     */
    ServerResponse queryOrderPayStatus(Integer userId,Long orderNo);
    
    /**
     * 创建订单
     * @param userId
     * @param shippingId
     * @return
     */
    ServerResponse createOrder(Integer userId,Integer shippingId);
    
    
    ServerResponse<String> cancel(Integer userId,Long orderNo);
    ServerResponse getOrderCartProduct(Integer userId);
    ServerResponse<OrderVo> getOrderDetail(Integer userId, Long orderNo);
    ServerResponse<PageInfo> getOrderList(Integer userId, int pageNum, int pageSize);



    //backend
    ServerResponse<PageInfo> manageList(int pageNum,int pageSize);
    ServerResponse<OrderVo> manageDetail(Long orderNo);
    ServerResponse<PageInfo> manageSearch(Long orderNo,int pageNum,int pageSize);
    ServerResponse<String> manageSendGoods(Long orderNo);
    
    //定时关单,对规定时间内未付款的订单进行关闭，
    void closeOrder(Integer hour);
    
}
