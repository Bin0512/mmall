package com.jike.common;
/**
 * 构建Redis连接池
 * @author Administrator
 *
 */

import java.util.ArrayList;
import java.util.List;

import com.jike.util.PropertiesUtil;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.util.Hashing;
import redis.clients.util.Sharded;

/**
 * 设置Redis连接池的相关属性
 * 确保能够连接到服务器上的Redis
 * 建立与服务器的通讯
 * @author Administrator
 *
 */
public class RedisShardedPool {
	//Jedis连接池
	private static ShardedJedisPool pool;
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
	
	
	//如果想进行redis扩容并使用，可以在用“，”分别对ip和端口进行分割，那么在初始化里，要引用集合遍历ip和端口，并一一对应上，再将info加入到集合中去。
	
	//设置redis所在的服务器主机地址
	private static String redis1Ip = PropertiesUtil.getProperty("redis1.ip");
	private static String redis2Ip = PropertiesUtil.getProperty("redis2.ip");
	//设置redis所在服务器开放的端口号
	private static Integer redis1Port = Integer.parseInt(PropertiesUtil.getProperty("redis1.port"));
	private static Integer redis2Port = Integer.parseInt(PropertiesUtil.getProperty("redis2.port"));
	
	
	private static void init() {
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(maxTotal);
		config.setMaxIdle(maxIdle);
		config.setMinIdle(minIdle);
		config.setTestOnBorrow(testOnBorrow);
		config.setTestOnReturn(testOnReturn);
		//连接耗尽时，是否阻塞，false会抛出异常，true阻塞直到超时，默认为true
		config.setBlockWhenExhausted(true);
		
		JedisShardInfo info1 = new JedisShardInfo(redis1Ip,redis1Port,1000*2);

        JedisShardInfo info2 = new JedisShardInfo(redis2Ip,redis2Port,1000*2);

        List<JedisShardInfo> jedisShardInfoList = new ArrayList<JedisShardInfo>(2);

        jedisShardInfoList.add(info1);
		jedisShardInfoList.add(info2);
		//毫秒为单位
		pool = new ShardedJedisPool(config, jedisShardInfoList, Hashing.MURMUR_HASH, Sharded.DEFAULT_KEY_TAG_PATTERN);
	}
	
	static {
		init();
	}
	
	//从连接池获取一个jedis实例
	public static ShardedJedis getJedis() {
		return pool.getResource();
	}
	
	//将实例返回给连接池
	public static void returnResource(ShardedJedis jedis) {
		pool.returnResource(jedis);
	}
	
	//将损坏的jedis实例返回给连接池
	public static void returnBrokenResource(ShardedJedis jedis) {
		pool.returnBrokenResource(jedis);
	}
	
	public static void main(String[] args) {
		System.out.println(redis1Ip);
		System.out.println(redis2Port);
		ShardedJedis jedis = pool.getResource();
		
		for(int i =0;i<10;i++){
            jedis.set("key"+i,"value"+i);
        }
		returnResource(jedis);
		//临时调用，销毁连接池中的所有连接，一般不会调用
		//pool.destroy();
		System.out.println("program is end");
	}
}



















