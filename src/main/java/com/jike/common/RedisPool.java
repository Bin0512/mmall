package com.jike.common;
/**
 * 构建Redis连接池
 * @author Administrator
 *
 */

import com.jike.util.PropertiesUtil;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisPool {
	//Jedis连接池
	private static JedisPool pool;
	//设置最大连接数
	private static Integer maxTotal = Integer.parseInt(PropertiesUtil.getProperty("redis.max.total", "20"));
	//设置最大空闲连接数
	private static Integer maxIdle = Integer.parseInt(PropertiesUtil.getProperty("redis.max.idle", "10"));
	//设置最小空闲连接数
	private static Integer minIdle = Integer.parseInt(PropertiesUtil.getProperty("redis.min.idle", "2"));
	//设置testOnBorrow，为true,代表在获取一个jedis实例时，验证其确实是可用的
	private static Boolean testOnBorrow = Boolean.parseBoolean(PropertiesUtil.getProperty("redis.test.borrow", "true"));
	//设置testOnReturn，为true,代表在放回一个jedis实例时，确认是可用的
	private static Boolean testOnReturn = Boolean.parseBoolean(PropertiesUtil.getProperty("redis.test.return", "true"));
	
	//设置redis所在的服务器主机地址
	private static String redisIp = PropertiesUtil.getProperty("redis.ip");
	//设置redis所在服务器开放的端口号
	private static Integer redisPort = Integer.parseInt(PropertiesUtil.getProperty("redis.port"));
	
	private static void init() {
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(maxTotal);
		config.setMaxIdle(maxIdle);
		config.setMinIdle(minIdle);
		config.setTestOnBorrow(testOnBorrow);
		config.setTestOnReturn(testOnReturn);
		//连接耗尽时，是否阻塞，false会抛出异常，true阻塞直到超时，默认为true
		config.setBlockWhenExhausted(true);
		//毫秒为单位
		pool = new JedisPool(config, redisIp, redisPort, 1000*2);
	}
	
	static {
		init();
	}
	
	//从连接池获取一个jedis实例
	public static Jedis getJedis() {
		return pool.getResource();
	}
	
	//将实例返回给连接池
	public static void returnResource(Jedis jedis) {
		pool.returnResource(jedis);
	}
	
	//将损坏的jedis实例返回给连接池
	public static void returnBrokenResource(Jedis jedis) {
		pool.returnBrokenResource(jedis);
	}
	
	public static void main(String[] args) {
		System.out.println(redisIp);
		System.out.println(redisPort);
		Jedis jedis = pool.getResource();
		jedis.set("harrykey", "harryvalue");
		returnResource(jedis);
		//临时调用，销毁连接池中的所有连接，一般不会调用
		//pool.destroy();
		System.out.println("program is end");
	}
}



















