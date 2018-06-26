package com.jike.util;

import com.jike.common.RedisPool;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

/**
 * jedis api 封装
 * @author Administrator
 *
 */
@Slf4j
public class RedisPoolUtil {
	
	//往redis里添加数据
	public static String set(String key,String value) {
		Jedis jedis = null;
		String result = null;
		try {
			jedis = RedisPool.getJedis();
			result = jedis.set(key, value);
		} catch (Exception e) {
			log.error("set key:{} value:{} error",key,value,e);
			RedisPool.returnBrokenResource(jedis);
			return result;
		}
		RedisPool.returnResource(jedis);
		return result;
	}
	
	//get方法
	public static String get(String key) {
		Jedis jedis = null;
		String result = null;
		try {
			jedis = RedisPool.getJedis();
			result = jedis.get(key);
		} catch (Exception e) {
			log.error("get value:{} error",key,e);
			RedisPool.returnBrokenResource(jedis);
			return result;
		}
		RedisPool.returnResource(jedis);
		return result;
	}
	
	//设置有效期，setEx
	public static String setEx(String key,String value,Integer time) {
		Jedis jedis = null;
		String result = null;
		try {
			jedis = RedisPool.getJedis();
			result = jedis.setex(key, time, value);
		} catch (Exception e) {
			log.error("setEx key:{} value:{} error",key,value,e);
			RedisPool.returnBrokenResource(jedis);
			return result;
		}
		RedisPool.returnResource(jedis);
		return result;
		
	}
	
	//设置键的有效期，expire
	public static Long expire(String key,Integer time) {
		Jedis jedis = null;
		Long result = null;
		try {
			jedis = RedisPool.getJedis();
			result = jedis.expire(key, time);
		} catch (Exception e) {
			log.error("expire key:{} error",key,e);
			RedisPool.returnBrokenResource(jedis);
			return result;
		}
		RedisPool.returnResource(jedis);
		return result;
	}
	
	//删除
	public static Long del(String key) {
		Jedis jedis = null;
		Long result = null;
		try {
			jedis = RedisPool.getJedis();
			result = jedis.del(key);
		} catch (Exception e) {
			log.error("del key:{} error",key,e);
			RedisPool.returnBrokenResource(jedis);
            return result;
		}
		RedisPool.returnResource(jedis);
		return result;
	}
	
	
	public static void main(String[] args) {
		Jedis jedis = RedisPool.getJedis();
		RedisPoolUtil.set("Harry", "Harry-value");
		String result = RedisPoolUtil.get("Harry");
		System.out.println(result);
		RedisPoolUtil.setEx("testEx", "testExValue", 60*10);
		RedisPoolUtil.expire("Harry", 60*20);
	}

}













