package com.jike.controller.portal;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.jike.common.Const;
import com.jike.common.RedisShardedPool;
import com.jike.common.ResponseCode;
import com.jike.common.ServerResponse;
import com.jike.pojo.User;
import com.jike.service.IUserService;
import com.jike.util.CookieUtil;
import com.jike.util.JsonUtil;
import com.jike.util.RedisPoolUtil;
import com.jike.util.RedisShardedPoolUtil;

import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/user/")
public class UserController {

	@Resource
	private IUserService iUserService;
	
	/**
     * 用户登录
     * @param username
     * @param password
     * @param session
     * @return
     */
    @RequestMapping(value = "login.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> login(@RequestParam(value="username",required = true)String username,@RequestParam(value="password",required = true)String password, HttpSession session,HttpServletResponse response){
       ServerResponse<User> serverResponse = iUserService.login(username, password);
       if (serverResponse.isSuccess()) {
    	   //session.setAttribute(Const.CURRENT_USER, serverResponse.getData());
    	   //System.out.println(session.getId());
    	   CookieUtil.writeLoginToken(response, session.getId());
    	   //RedisPoolUtil.setEx(session.getId(), JsonUtil.obj2String(serverResponse.getData()), Const.RedisCacheExtime.REDIS_SESSION_EXTIME);
    	   RedisShardedPoolUtil.setEx(session.getId(), JsonUtil.obj2String(serverResponse.getData()), Const.RedisCacheExtime.REDIS_SESSION_EXTIME);
       }
       return serverResponse;
    }

    @RequestMapping(value = "logout.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> logout(HttpServletRequest request,HttpServletResponse response){
    	String loginToken = CookieUtil.readLoginToken(request);
    	CookieUtil.delLoginToken(request, response);
    	RedisPoolUtil.del(loginToken);
        //session.removeAttribute(Const.CURRENT_USER);
        //在退出的时候只返回一个成功的状态码
        return ServerResponse.createBySuccess();
    }

    @RequestMapping(value = "register.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> register(User user){
        return iUserService.register(user);
    }

    @RequestMapping(value = "check_valid.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> checkValid(String str,String type){
    	
        return iUserService.checkValid(str, type);
    }
    
   
    
    @RequestMapping(value = "forget_get_question.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetRestPassword(String username){
    	
    	return iUserService.selectQuestion(username);
    }
    
    @RequestMapping(value = "forget_check_answer.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetCheckAnswer(String username,String question,String answer){
    	
    	return iUserService.checkAnswer(username, question, answer);
    }
    
    @RequestMapping(value = "forget_reset_password.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetRestPassword(String username,String passwordNew,String forgetToken){
        return iUserService.forgetResetPassword(username,passwordNew,forgetToken);
    }
    
    @RequestMapping(value = "reset_password.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> resetPassword(String passwordOld,String passwordNew,HttpSession session){
    	User user = (User) session.getAttribute(Const.CURRENT_USER);
    	if (user == null) {
			return ServerResponse.createByErrorMessage("用户未登录");
		}
    	return iUserService.resetPassword(passwordOld, passwordNew, user);
    }
    
    @RequestMapping(value = "update_information.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> updateInformation(HttpSession session,User user){
    	/*这里要注意：因为用户的用户名和id是不允许更改，所以要在这里设置好*/
    	User currentUser = (User) session.getAttribute(Const.CURRENT_USER);
    	if (user == null) {
			return ServerResponse.createByErrorMessage("用户未登录");
		}
    	user.setId(currentUser.getId());
    	user.setUsername(currentUser.getUsername());
    	ServerResponse<User> response = iUserService.updateInformation(user);
    	//修改成功后，要及时更新Session中的用户信息
    	if (response.isSuccess()) {
    		response.getData().setUsername(currentUser.getUsername());
			session.setAttribute(Const.CURRENT_USER, response.getData());
		}
    	return response;
    }
    
    //10.获取当前登录用户的详细信息，并强制登录 /user/get_information.do
    @RequestMapping(value = "get_information.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> get_information(HttpSession session){
        User currentUser = (User)session.getAttribute(Const.CURRENT_USER);
        if(currentUser == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"未登录,需要强制登录status=10");
        }
        return iUserService.getInformation(currentUser.getId());
    }
    
    //4.获取登录用户信息 /user/get_user_info.do
    @RequestMapping(value = "get_user_info.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> getUserInfo(HttpServletRequest request){
    	//获取cookie的值，即用户登录的sessionid
    	String cookie = CookieUtil.readLoginToken(request);
    	if (StringUtils.isEmpty(cookie)) {
    		return ServerResponse.createByErrorMessage("用户未登录,无法获取当前用户信息");
		}
    	//根据cookie的值，从redis中获取用户信息（字符串）
    	String userInfo = RedisPoolUtil.get(cookie);
    	//反序列化为对象
    	User user = JsonUtil.string2Object(userInfo, User.class);
    	//User user = (User)session.getAttribute(Const.CURRENT_USER);
    	if (user == null) {
			return ServerResponse.createByErrorMessage("用户未登录,无法获取当前用户信息");
		}
    	return ServerResponse.createBySuccess(user);
    }
    
}
