package com.jike.controller.common;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import com.jike.common.Const;
import com.jike.pojo.User;
import com.jike.util.CookieUtil;
import com.jike.util.JsonUtil;
import com.jike.util.RedisPoolUtil;

public class SessionExpireFilter implements Filter {

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		String loginToken = CookieUtil.readLoginToken(request);
		if (StringUtils.isNotEmpty(loginToken)) {
			String userInfo = RedisPoolUtil.get(loginToken);
			User user = JsonUtil.string2Object(userInfo, User.class);
			if (user != null) {
				RedisPoolUtil.expire(loginToken, Const.RedisCacheExtime.REDIS_SESSION_EXTIME);
			}
		}
		filterChain.doFilter(servletRequest, servletResponse);
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub

	}

}
