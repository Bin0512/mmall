package com.jike.common;

import java.io.Serializable;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include =  JsonSerialize.Inclusion.NON_NULL)
//保证序列化json的时候,如果是null的对象,key也会消失
public class ServerResponse<T> implements Serializable {
	
	//状态码，Forexample:成功or失败
	private int status;
	
	//返回信息，Forexample:"注册成功/失败" "用户名不存在"等等；
	private String message;
	
	//返回数据
	private T data;
	
	public int getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}

	public T getData() {
		return data;
	}

	private ServerResponse(int status) {
		super();
		this.status = status;
	}
	
	private ServerResponse(T data) {
		super();
		this.data = data;
	}

	private ServerResponse(int status, T data) {
		super();
		this.status = status;
		this.data = data;
	}

	private ServerResponse(int status, String message) {
		super();
		this.status = status;
		this.message = message;
	}

	private ServerResponse(int status, String message, T data) {
		super();
		this.status = status;
		this.message = message;
		this.data = data;
	}
	
	//用于判断响应成功、失败状态码
	@JsonIgnore
	//使之不在json序列化结果当中
	public boolean isSuccess() {
		
		return this.status == ResponseCode.SUCCESS.getCode();
	}
	
	public static <T> ServerResponse<T> createBySuccess(){
		
		return new ServerResponse<T>(ResponseCode.SUCCESS.getCode());
	}
	
	public static <T> ServerResponse<T> createBySuccessMessage(String message){
		
		return new ServerResponse<T>(ResponseCode.SUCCESS.getCode(), message);
	}
	
	public static <T> ServerResponse<T> createBySuccess(T data){
		
		return new ServerResponse<T>(ResponseCode.SUCCESS.getCode(), data);
	}
	
	public static <T> ServerResponse<T> createBySuccess(String message,T data){
		
		return new ServerResponse<T>(ResponseCode.SUCCESS.getCode(),message, data);
	}
	
	public static<T> ServerResponse<T> createByError(){
		
		return new ServerResponse<T>(ResponseCode.ERROR.getCode());
	}
	
	//错误响应之后返回错误信息
    public static <T> ServerResponse<T> createByErrorMessage(String errorMessage){
        return new ServerResponse<T>(ResponseCode.ERROR.getCode(),errorMessage);
    }
	
    //解决，类似需要登录 和  参数错误的 提示方法 ，因为他们对应不同的错误码，在枚举类型中已定义
    public static <T> ServerResponse<T> createByErrorCodeMessage(int errorCode,String errorMessage){
        return new ServerResponse<T>(errorCode,errorMessage);
    }
	
	

}
