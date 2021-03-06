package com.jike.common;



import java.awt.RenderingHints.Key;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;



public class TokenCache {

    private static Logger logger = LoggerFactory.getLogger(TokenCache.class);

    public static final String TOKEN_PREFIX = "token_";
    
    //LRU算法	设置缓存初始化大小为1000，最大容量为10000,有效期为12个小时
    private static LoadingCache<String, String> localCache = CacheBuilder.newBuilder().initialCapacity(1000).maximumSize(10000).expireAfterAccess(12, TimeUnit.HOURS)
    		.build(new CacheLoader<String, String>(){

				@Override
				public String load(String arg0) throws Exception {
					// TODO Auto-generated method stub
					return "null";
				}
    		});

    public static void setKey(String key,String value) {
    	localCache.put(key, value);
    }
    
    public static String getKey(String key) {
    	String value = null;
    	try {
			value = localCache.get(key);
			if ("null".equals(value)) {
				return null;
			}
			return value;
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			logger.error("localCache get error",e);
		}
    	return null;
    }
    //LRU算法    本地缓存初始化大小1000，最大容量10000，有效期为12个小时
    /*private static LoadingCache<String,String> localCache = CacheBuilder.newBuilder().initialCapacity(1000).maximumSize(10000).expireAfterAccess(12, TimeUnit.HOURS)
            .build(new CacheLoader<String, String>() {//默认加载实现
                //默认的数据加载实现,当调用get取值的时候,如果key没有对应的值,就调用这个方法进行加载.
                @Override
                public String load(String s) throws Exception {
                    return "null";
                }
            });*/

  /*  public static void setKey(String key,String value){
        localCache.put(key,value);
    }

    public static String getKey(String key){
        String value = null;
        try {
            value = localCache.get(key);
            if("null".equals(value)){
                return null;
            }
            return value;
        }catch (Exception e){
            logger.error("localCache get error",e);
        }
        return null;
    }*/
}
