package com.jike.common;

public enum ResponseCode {
	
	/**
	 * 设置状态码
	 */
    SUCCESS(0,"SUCCESS"),//成功
    ERROR(1,"ERROR"),//失败
    NEED_LOGIN(10,"NEED_LOGIN"),//需要登录
    ILLEGAL_ARGUMENT(2,"ILLEGAL_ARGUMENT");//参数错误
	
	private final int code; //状态码
	
	private final String desc; //状态描述
	
	private ResponseCode(int code, String desc) {
		this.code = code;
		this.desc = desc;
	}

	public int getCode() {
		return code;
	}

	public String getDesc() {
		return desc;
	}
	
	
	
}
