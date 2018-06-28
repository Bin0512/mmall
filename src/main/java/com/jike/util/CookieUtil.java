package com.jike.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 将用户登录的sessionid写入cookie中，存在客户端
 * @author Administrator
 *
 */
@Slf4j
public class CookieUtil {
	
	//设置cookie的域，这里代表所有以“.harrymall.com”结尾的域名都能访问此cookie
	private final static String COOKIE_DOMAIN = ".harrymmall.com";
	//设置cookie的键名
	private final static String COOKIE_NAME = "mmall_login_token";
	
	//写cookie
	public static void writeLoginToken(HttpServletResponse response,String token) {
		Cookie cookie = new Cookie(COOKIE_NAME, token);
		cookie.setDomain(COOKIE_DOMAIN);
		cookie.setPath("/");//代表设置在根目录，项目下的所有模块都能访问
		cookie.setHttpOnly(true);//防止脚本攻击带来的信息泄露风险，https://blog.csdn.net/zmx729618/article/details/51461261
		//如果这个maxage不设置的话，cookie就不会写入硬盘，而是写在内存。只在当前页面有效。
		cookie.setMaxAge(60 * 60 * 24 * 365);//如果是-1，代表永久
		log.info("write cookieName:{},cookieValue:{}",cookie.getName(),cookie.getValue());
		response.addCookie(cookie);
	}
	
	//读cookie
	public static String readLoginToken(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				log.info("read cookieName:{},cookieValue:{}",cookie.getName(),cookie.getValue());
				if (StringUtils.equals(cookie.getName(), COOKIE_NAME)) {
					log.info("return cookieName:{},cookieValue:{}",cookie.getName(),cookie.getValue());
					return cookie.getValue();
				}
			}
		}
		return null;
	}
	
	//删除cookie
	public static void delLoginToken(HttpServletRequest request,HttpServletResponse response) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (StringUtils.equals(cookie.getName(), COOKIE_NAME)) {
					cookie.setDomain(COOKIE_DOMAIN);
                    cookie.setPath("/");
                    cookie.setMaxAge(0);//设置成0，代表删除此cookie。
                    log.info("del cookieName:{},cookieValue:{}",cookie.getName(),cookie.getValue());
                    response.addCookie(cookie);
                    return;
				}
			}
		}
	}
}














