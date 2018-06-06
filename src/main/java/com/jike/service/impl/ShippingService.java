package com.jike.service.impl;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.jike.common.ServerResponse;
import com.jike.dao.ShippingMapper;
import com.jike.pojo.Shipping;
import com.jike.service.IShippingService;

@Service("iShippingService")
public class ShippingService implements IShippingService {

	@Resource
	private ShippingMapper shippingMapper;

	public ServerResponse add(Integer userId, Shipping shipping) {
		shipping.setUserId(userId);
		int rowCount = shippingMapper.insert(shipping);
		if (rowCount > 0) {
			// 新建成功后，把新地址的ID返回给前端。
			Map result = Maps.newHashMap();
			result.put("shippingId", shipping.getId());
			return ServerResponse.createBySuccess("新建地址成功", result);
		}
		return ServerResponse.createByErrorMessage("新建地址失败");
	}

	public ServerResponse<String> del(Integer userId, Integer shippingId) {
		int resultCount = shippingMapper.deleteByShippingIdUserId(userId, shippingId);
		if (resultCount > 0) {
			return ServerResponse.createBySuccess("删除地址成功");
		}
		return ServerResponse.createByErrorMessage("删除地址失败");
	}

	public ServerResponse update(Integer userId, Shipping shipping) {
		// 这里之所以要重新赋值一下，就是为了避免横向越权的问题。
		// 防止登录的用户直接通过调用接口的方式，传进一个不是自己的userId，从而达到修改别人收货地址信息的目的。
		shipping.setUserId(userId);
		int rowCount = shippingMapper.updateByShipping(shipping);
		if (rowCount > 0) {
			return ServerResponse.createBySuccess("更新地址成功");
		}
		return ServerResponse.createByErrorMessage("更新地址失败");
	}

	public ServerResponse<Shipping> select(Integer userId, Integer shippingId) {
		Shipping shipping = shippingMapper.selectByShippingIdUserId(userId, shippingId);
		if (shipping == null) {
			return ServerResponse.createByErrorMessage("无法查询到该地址");
		}
		return ServerResponse.createBySuccess("更新地址成功", shipping);
	}

	public ServerResponse<PageInfo> list(Integer userId, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		List<Shipping> shippingList = shippingMapper.selectByUserId(userId);
		PageInfo pageInfo = new PageInfo(shippingList);
		return ServerResponse.createBySuccess(pageInfo);
	}

}
