package com.atguigu.tingshu.common.cache;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import com.atguigu.tingshu.common.constant.RedisConstant;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
@Aspect
public class GuiGuCacheAspect {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @Around("@annotation(guiGuCache)")
    public Object around(ProceedingJoinPoint pjp, GuiGuCache guiGuCache) {
        try {

            //构建redisKey
            String redisKey = guiGuCache.prefix();
            List<Object> argList = Arrays.asList(pjp.getArgs());
            if (CollectionUtil.isNotEmpty(argList)) {
                //方法中含有参数
                String argStr = argList.stream().map(Object::toString).collect(Collectors.joining("_"));
                redisKey += argStr;
            } else {
                //获取方法的签名
                String name = pjp.getSignature().getName();
                redisKey += name;
            }
            //1.从redis中获取数据
            Object result = redisTemplate.opsForValue().get(redisKey);
            if (result != null) {
                //2.如果数据存在则直接返回
                return result;
            } else {
                //3.如果数据不存在则获取分布式锁
                String lockKey = redisKey + RedisConstant.CACHE_LOCK_SUFFIX;
                RLock lock = redissonClient.getLock(lockKey);
                try {
                    //获取锁
                    boolean flag = lock.tryLock(1, TimeUnit.SECONDS);
                    if (flag) {
                        //4.获取锁成功后，执行目标方法，将方法的结果放入缓存， 最后释放锁并返回结果
                        Object proceed = pjp.proceed();
                        long ttl = RandomUtil.randomInt(500, 600) + guiGuCache.ttl();
                        redisTemplate.opsForValue().set(redisKey, proceed, ttl, guiGuCache.timeunit());
                        return proceed;
                    } else {
                        TimeUnit.MICROSECONDS.sleep(50);
                        //5.获取锁失败则进行自旋
                        return this.around(pjp, guiGuCache);
                    }
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                } finally {
                    lock.unlock();
                }
            }
        } catch (RuntimeException e) {
            //6.兜底执行目标方法返回结果 获取锁失败后
            try {
                log.info("获取锁失败，执行兜底方法查询数据库返回数据");
                return pjp.proceed();
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }

    }
}
