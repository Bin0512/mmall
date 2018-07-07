package com.jike.task;

import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.jike.common.Const;
import com.jike.common.RedissonManager;
import com.jike.service.IOrderService;
import com.jike.util.DateTimeUtil;
import com.jike.util.PropertiesUtil;
import com.jike.util.RedisShardedPoolUtil;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CloseOrderTask {

	@Resource
	private IOrderService iOrderService;
	
	@Resource
	private RedissonManager redisson;
	
	
	/**
	 * 版本演进
	 * V1:
	 * 	更适用于单服务器下的单个应用。放在tomcat集群下的结果是，多个服务器都会执行这个定时关单的方法，会影响服务器和数据库的性能。
	 * 	问题：在集群环境下，如何做到只让一台服务器执行定时关单任务从而提高程序性能呢？	解决方案：使用Redis分布式锁
	 * V2:
	 * 	使用了redis分布式锁，解决了V1版本存在的问题，但又带了一个新的问题：死锁。所谓死锁，就是在缓存中，未设置锁的有效期，从而导致锁永不过期的情况，即为“-1”，导致其它进程永远无法获取锁
	 * 	实际业务场景：
	 * 		在我们某一次获取锁成功后，即执行了setnx,还没来得及设置锁的有效期的时候，突然服务器奔溃了，或者网络断开了等意外情况发生，
	 * 		这样就出现了死锁，等你服务器再重新启动的时候，是获取不到锁的。
	 * 	解决方案：@PreDestroy
	 * 	关闭Tomcat有两种方式：
	 * 		1、找到Tomcat运行的进程号，直接kill-----粗暴
	 * 		2、调用tomcat的shutdown方法，
	 * V3:
	 * 	设置了双重防死锁，else、expire
	 * 	Redis分布式锁的优化版
	 *  关键思路在于，当我们获取锁失败的时候，会通过get获取锁的值，因为我们一开始用了时间戳作为锁的值，在这里就能派上用场了，
	 * 
	 * V4（推荐）:
	 * 	使用Redisson框架来构建分布式锁，操作更简洁。注意wait_time要设置为0，在我们无法预估其后的业务逻辑代码会执行多长时间的时候，保守起见要设置为0，防止出现两个进程同时获取到锁的情况
	 * 	使用了Redisson就不用像V3那样复杂的判断了。
	 */
	@PreDestroy
	public void delLock(){//因为加了这个注解的原因，当不使用粗暴方式关闭tomcat（即kill进程）的时候，tomcat会调用加了这个注解的方法来shutdown
        RedisShardedPoolUtil.del(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
    }
	
	
	//@Scheduled(cron = "0 */1 * * * ?")//代表每分钟执行一次
	public void closeOrderTaskV1() {
		log.info("定时关闭订单任务启动");
		//设置对多长时间的订单进行关闭
		int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.time.hour", "2"));
		iOrderService.closeOrder(hour);
		log.info("定时关闭订单任务完毕");
	}
	
	//@Scheduled(cron = "0 */1 * * * ?")
	public void closeOrderTaskV2() {
		log.info("定时关闭订单任务启动");
		//设置锁的超时时间,5秒
		long lockTimeout = Long.parseLong(PropertiesUtil.getProperty("lock.timeout", "5000")); 
		
		//获取锁,1成功，0失败
		Long setnxResult = RedisShardedPoolUtil.setnx(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK, String.valueOf(System.currentTimeMillis()+lockTimeout));
		if (setnxResult != null && setnxResult.intValue() == 1) {
			
		}else {
			log.info("没有获得分布式锁:{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
		}
		log.info("定时关闭订单任务完毕");
	}
	
	//@Scheduled(cron = "0 */1 * * * ?")
	public void closeOrderTaskV3(){
        log.info("关闭订单定时任务启动");
        long lockTimeout = Long.parseLong(PropertiesUtil.getProperty("lock.timeout","5000"));
        Long setnxResult = RedisShardedPoolUtil.setnx(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,String.valueOf(System.currentTimeMillis()+lockTimeout));
        if(setnxResult != null && setnxResult.intValue() == 1){
            closeOrder(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        }else{
        	//获取锁的值-------时间戳（设置锁的当前时间+超时时间）
            String lockValueA = RedisShardedPoolUtil.get(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
            //如果当前的时间大于锁的时间戳，代表可以重新设置锁的值，也就能够获取到锁，否则依然不能获取锁，因为锁还未超时,要继续再等等。但总归是能等到的，不像V2版的，锁直接是永久，怎么也不会超时，等再久也白搭
            if (lockValueA != null && System.currentTimeMillis() > Long.parseLong(lockValueA)) {
				String localValueB = RedisShardedPoolUtil.getSet(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK, String.valueOf(System.currentTimeMillis() + lockTimeout));
				/**
				 * localValueB == null-----代表在重新设置锁的时候，原来的锁刚好也过了超时时间或者其他进程释放了锁
				 * localValueB != null && StringUtils.equals(lockValueA, localValueB) 
				 * ----代表没有其他进程获取到锁，从locaValueA起，一直是由本进程在操作同一把锁，当然也就能对它进行重新设置，否则又获取不到锁，又要等占了这个锁的进程释放之后或者锁超时才能有机会获取。
				 *
				 *redis的getset方法，如果没有旧值旧会返回nil，即：空，这里我们set了一个新的value值，获取旧的值。
				 */
				if (localValueB == null || (localValueB != null && StringUtils.equals(lockValueA, localValueB))) {
					//真正获取到锁
					closeOrder(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
				}else {
					log.info("没有获取到分布式锁:{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
				}
            }else {
				log.info("没有获取到分布式锁:{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
			}
        }
        log.info("关闭订单定时任务结束");
    }
	
	@Scheduled(cron = "0 */1 * * * ?")
	public void closeOrderTaskV4() {
		//通过锁的名字设置锁
		RLock lock = redisson.getRedisson().getLock(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
		Boolean getLock = false;

		try {
			//竞争锁,返回true，代表获取到锁。设置的wait_time为0秒，5秒有效期
			if (getLock = lock.tryLock(0, 5, TimeUnit.SECONDS)) {
				log.info("Redisson获取到分布式锁:{},ThreadName:{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,Thread.currentThread().getName());
				int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.time.hour","2"));
		        //iOrderService.closeOrder(hour); //如果只是测试分布式锁的获取，可以把这个注释掉
			}else {
				log.info("Redisson没有获取到分布式锁:{},ThreadName:{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,Thread.currentThread().getName());
			}
		} catch (InterruptedException e) {
			log.error("Redisson分布式锁获取异常",e);
		}finally {
			if (!getLock) {
				return;
			}
			lock.unlock();//释放锁
			log.info("Redisson分布式锁释放锁");
		}
	}
	
	
	private void closeOrder(String lockName) {
		RedisShardedPoolUtil.expire(lockName, 5);//设置锁的有效期5秒，防止死锁
		log.info("获取{},ThreadName:{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,Thread.currentThread().getName());
        int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.time.hour","2"));
        iOrderService.closeOrder(hour); //如果只是测试分布式锁的获取，可以把这个注释掉
        RedisShardedPoolUtil.del(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        log.info("释放{},ThreadName:{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,Thread.currentThread().getName());
        log.info("===============================");
	}
	
}
























