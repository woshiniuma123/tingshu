package com.atguigu.tingshu;

import com.atguigu.tingshu.common.constant.RedisConstant;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
@Slf4j
public class ServiceAlbumApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(ServiceAlbumApplication.class, args);
    }


    @Autowired
    private RedissonClient redissonClient;

    /**
     * 初始化一个布隆过滤器
     *
     * @param args
     * @throws Exception
     */
    @Override
    public void run(String... args) throws Exception {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        boolean exists = bloomFilter.isExists();
        if (!exists) {
            log.info("布隆过滤器不存在开始创建");
            //参数1：布隆过滤器的容量
            //参数2：布隆过滤器的误判率
            boolean flag = bloomFilter.tryInit(10000L, 0.03);
            log.info("布隆过滤器初始化成功");
        }
    }
}
