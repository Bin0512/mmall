package com.jike.service;

import com.jike.common.ServerResponse;
import com.jike.pojo.User;

public interface IUserService {

	/**
	 * 登录
	 * @param username
	 * @param password
	 * @return
	 */
	ServerResponse<User> login(String username, String password);

	/**
	 * 注册
	 * @param user
	 * @return
	 */
    ServerResponse<String> register(User user);

    /**
     * 验证接口（密码、邮箱）
     * @param str
     * @param type
     * @return
     */
    ServerResponse<String> checkValid(String str,String type);
    
    /**
     * 根据用户名查找问题
     * @param username
     * @return
     */
    ServerResponse selectQuestion(String username);

    /**
     * 验证答案
     * @param username
     * @param question
     * @param answer
     * @return
     */
    ServerResponse<String> checkAnswer(String username,String question,String answer);

    /**
     * 忘记密码中的重值密码，用户未登录
     * @param username
     * @param passwordNew
     * @param forgetToken
     * @return
     */
    ServerResponse<String> forgetResetPassword(String username,String passwordNew,String forgetToken);
    
    /**
     * 用户登录之后，对密码进行修改
     * @param passwordOld
     * @param passwordNew
     * @param user
     * @return
     */
    ServerResponse<String> resetPassword(String passwordOld,String passwordNew,User user);

    /**
     * 更新用户信息
     * @param user
     * @return
     */
    ServerResponse<User> updateInformation(User user);

    /**
     * 获取用户信息
     * @param userId
     * @return
     */
    ServerResponse<User> getInformation(Integer userId);

    /**
     * 验证是否是管理员身份
     * @param user
     * @return
     */
    ServerResponse checkAdminRole(User user);
    
}
