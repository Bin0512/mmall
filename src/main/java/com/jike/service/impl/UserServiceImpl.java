package com.jike.service.impl;

import java.util.UUID;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.jike.common.Const;
import com.jike.common.ServerResponse;
import com.jike.common.TokenCache;
import com.jike.dao.UserMapper;
import com.jike.pojo.User;
import com.jike.service.IUserService;
import com.jike.util.MD5Util;

@Service("iUserService")
public class UserServiceImpl implements IUserService {

	@Resource
	private UserMapper userMapper;
	
	public ServerResponse<User> login(String username, String password) {
		//1、验证用户名是否存在
		int resultCount = userMapper.checkUsername(username);
		if (resultCount == 0) {
			return ServerResponse.createByErrorMessage("用户名不存在");
		}
		//对传入的密码进行md5加密，再跟数据库进行比对
		String md5password = MD5Util.MD5EncodeUtf8(password);
		//2、验证密码是否正确
		User user = userMapper.selectLogin(username, md5password);
		if (user == null) {
			return ServerResponse.createByErrorMessage("密码错误");
		}
		//3、验证通过后，处于安全考虑需要把用户密码置为空
		user.setPassword(StringUtils.EMPTY);
		return ServerResponse.createBySuccess("登录成功", user);
	}

	public ServerResponse<String> register(User user) {
		//1、验证用户名、邮箱是否已经存在，如果存在就不能再注册
		ServerResponse validResponse = this.checkValid(user.getUsername(), Const.USERNAME);
		if (!validResponse.isSuccess()) {
			return validResponse;
		}
		validResponse = this.checkValid(user.getEmail(), Const.EMAIL);
		if (!validResponse.isSuccess()) {
			return validResponse;
		}
    	//2、给注册的用户分配角色，管理员还是普通用户
    	user.setRole(Const.Role.ROLE_CUSTOMER);
    	//3、对用户的密码进行MD5加密
    	user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));
    	//4、往数据库添加数据，成功or失败 
    	int count = userMapper.insert(user);
    	if (count == 0) {
			return ServerResponse.createByErrorMessage("注册失败");
		}
		return ServerResponse.createBySuccessMessage("注册成功");
	}

	public ServerResponse<String> checkValid(String str, String type) {
		int resultCount = 0;
		//不为空，且不能为空格
		if (StringUtils.isNotBlank(type)) {
			//验证邮箱
			if (Const.EMAIL.equals(type)) {
				resultCount = userMapper.checkEmail(str);
				if (resultCount > 0) {
					return ServerResponse.createByErrorMessage("邮箱已经存在");
				}
			}
			//验证用户名
			if (Const.USERNAME.equals(type)) {
				resultCount = userMapper.checkUsername(str);
				if (resultCount > 0) {
					return ServerResponse.createByErrorMessage("用户名已经存在");
				}
			}
		}else {
			return ServerResponse.createByErrorMessage("参数错误");
		}
		return ServerResponse.createBySuccessMessage("校验成功");
	}

	@Override
	public ServerResponse selectQuestion(String username) {
		//1、验证用户名是否存在
    	ServerResponse valideResponse = this.checkValid(username, Const.USERNAME);
    	if (valideResponse.isSuccess()) {
			return ServerResponse.createByErrorMessage("用户名不存在");
		}
    	//2、如果用户存在再根据用户名获取相应的问题
    	String question = userMapper.selectQuestionByUsername(username);
    	//3、问题不为空，将问题返回，如果为空，则返回提示信息“找回密码的问题是空的”
    	if (StringUtils.isNotBlank(question)) {
			return ServerResponse.createBySuccess(question);
		}
		return ServerResponse.createByErrorMessage("找回密码的问题是空的");
	}

	@Override
	public ServerResponse<String> checkAnswer(String username, String question, String answer) {
		//1、通过数据库对答案进行验证
		int resultCount = userMapper.checkAnswer(username, question, answer);
		if (resultCount > 0) {
			//2、生成唯一验证码
			String fogetToken = UUID.randomUUID().toString();
			//3、将验证码放入缓存
			TokenCache.setKey(TokenCache.TOKEN_PREFIX + username, fogetToken);
			return ServerResponse.createBySuccess(fogetToken);
		}
		return ServerResponse.createByErrorMessage("答案错误");
	}

	@Override
	public ServerResponse<String> forgetResetPassword(String username, String passwordNew, String forgetToken) {
		//1、对传入的Token进行非空判断
		if (StringUtils.isBlank(forgetToken)) {
			return ServerResponse.createByErrorMessage("参数错误，请传入Token值");
		}
		//2、对传入的用户名进行验证，判断是否存在
		ServerResponse validResponse = this.checkValid(username, Const.USERNAME);
		if (validResponse.isSuccess()) {
			return ServerResponse.createByErrorMessage("用户名不存在");
		}
		//3、从缓存中获取Token、
		String token = TokenCache.getKey(TokenCache.TOKEN_PREFIX + username);
		//4、判断token是否过期
		if (StringUtils.isBlank(token)) {
			return ServerResponse.createByErrorMessage("Token失效");
		}
		//5、将获取到的fogetToken与从缓存中获取的Token进行比对
		if (StringUtils.equals(forgetToken, token)) {
			//6、匹配成功，对新密码进行MD5加密并存储到数据库中去
			String md5password = MD5Util.MD5EncodeUtf8(passwordNew);
			int updateCount = userMapper.updatePasswordByUsername(username, md5password);
			if (updateCount > 0) {
				return ServerResponse.createBySuccessMessage("密码修改成功");
			}
		}else {
			return ServerResponse.createByErrorMessage("token错误,请重新获取重置密码的token");
		}
		return ServerResponse.createByErrorMessage("密码修改失败");
	}

	@Override
	public ServerResponse<String> resetPassword(String passwordOld, String passwordNew, User user) {
    	//1、对旧密码进行校验
    	int resultCount = userMapper.checkPassword(MD5Util.MD5EncodeUtf8(passwordOld), user.getId());
    	if (resultCount == 0) {
			return ServerResponse.createByErrorMessage("您的旧密码有误");
		}
    	//2、对新密码进行加密
    	user.setPassword(MD5Util.MD5EncodeUtf8(passwordNew));
    	//3、将数据修改进数据库
		int updateCount = userMapper.updateByPrimaryKey(user);
		if (updateCount > 0) {
			return ServerResponse.createBySuccessMessage("密码更新成功");
		}
		return ServerResponse.createByErrorMessage("密码更新失败");
	}

	@Override
	public ServerResponse<User> updateInformation(User user) {
		//1、校验新邮箱是否已经存在，因为不能更改username，所以此处无需进行校验
    	ServerResponse valideResponse = this.checkValid(user.getEmail(), Const.EMAIL);
    	if (!valideResponse.isSuccess()) {
			return ServerResponse.createByErrorMessage("email已存在,请更换email再尝试更新");
		}
    	//2、检验通过后将信息更新到数据库,用户名、密码不用修改，所以此处重新实例化一个User对象
    	User updateUser = new User();
    	updateUser.setId(user.getId());
    	updateUser.setEmail(user.getEmail());
        updateUser.setPhone(user.getPhone());
        updateUser.setQuestion(user.getQuestion());
        updateUser.setAnswer(user.getAnswer());
		int updateCount = userMapper.updateByPrimaryKeySelective(updateUser);
		if (updateCount > 0) {
			return ServerResponse.createBySuccess("更新个人信息成功", updateUser);
		}
		return ServerResponse.createByErrorMessage("更新个人信息失败");
	}

	@Override
	public ServerResponse<User> getInformation(Integer userId) {
		//1、根据userId从数据库中获取数据，判断是否存在
    	User user = userMapper.selectByPrimaryKey(userId);
    	//2、如果存在，处于安全考虑需要将用户密码置为空
		if (user == null) {
			return ServerResponse.createByErrorMessage("用户不存在");
		}
		user.setPassword(StringUtils.EMPTY);
		return ServerResponse.createBySuccess(user);
	}

	@Override
	public ServerResponse checkAdminRole(User user) {
		//判断用户角色 即可,只需返回带状态码的响应对象就可以了
		if (user != null && user.getRole().intValue() == Const.Role.ROLE_ADMIN) {
			return ServerResponse.createBySuccess();
		}
		return ServerResponse.createByError();
	}

	

}
