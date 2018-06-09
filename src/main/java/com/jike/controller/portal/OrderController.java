package com.jike.controller.portal;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.jike.common.Const;
import com.jike.common.ResponseCode;
import com.jike.common.ServerResponse;
import com.jike.pojo.User;
import com.jike.service.IOrderService;

@Controller
@RequestMapping("/order/")
public class OrderController {
	
	@Resource
	private IOrderService orderService;

	@RequestMapping("pay.do")
	@ResponseBody
	public ServerResponse pay(HttpSession session,HttpServletRequest request,Long orderNo) {
		User user = (User) session.getAttribute(Const.CURRENT_USER);
		if (user == null) {
			return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
		}
		//二维码存放目录
		String path = request.getSession().getServletContext().getRealPath("upload");
		return orderService.pay(orderNo, user.getId(), path);
	}
	
	
}
