package com.jike.controller.portal;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.github.pagehelper.PageInfo;
import com.jike.common.Const;
import com.jike.common.ResponseCode;
import com.jike.common.ServerResponse;
import com.jike.pojo.Shipping;
import com.jike.pojo.User;
import com.jike.service.IShippingService;

@Controller
@RequestMapping("/shipping/")
public class ShippingController {

	@Resource
	private IShippingService iShippingService;
	
	@RequestMapping("add.do")
	@ResponseBody
	public ServerResponse add(HttpSession session, Shipping shipping) {
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),
					ResponseCode.NEED_LOGIN.getDesc());
		}
		return iShippingService.add(user.getId(), shipping);
	}

	@RequestMapping("del.do")
	@ResponseBody
	public ServerResponse del(HttpSession session, Integer shippingId) {
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),
					ResponseCode.NEED_LOGIN.getDesc());
		}
		return iShippingService.del(user.getId(), shippingId);
	}

	@RequestMapping("update.do")
	@ResponseBody
	public ServerResponse update(HttpSession session, Shipping shipping) {
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),
					ResponseCode.NEED_LOGIN.getDesc());
		}
		return iShippingService.update(user.getId(), shipping);
	}

	@RequestMapping("select.do")
	@ResponseBody
	public ServerResponse<Shipping> select(HttpSession session, Integer shippingId) {
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),
					ResponseCode.NEED_LOGIN.getDesc());
		}
		return iShippingService.select(user.getId(), shippingId);
	}

	@RequestMapping("list.do")
	@ResponseBody
	public ServerResponse<PageInfo> list(@RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
			@RequestParam(value = "pageSize", defaultValue = "10") int pageSize, HttpSession session) {
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),
					ResponseCode.NEED_LOGIN.getDesc());
		}
		return iShippingService.list(user.getId(), pageNum, pageSize);
	}
}
