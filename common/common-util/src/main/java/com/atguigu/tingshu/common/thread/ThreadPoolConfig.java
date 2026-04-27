package com.atguigu.tingshu.common.thread;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.*;

@Configuration
@Slf4j
public class ThreadPoolConfig {


    @Bean
    public Executor threadPoolExecutor() {
        //获取cpu核心数获取核心线程数
        int cupCoreCount = Runtime.getRuntime().availableProcessors();
        int threadCount = cupCoreCount * 2;

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                threadCount,
                threadCount,
                0,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
        threadPoolExecutor.prestartCoreThread();
        return threadPoolExecutor;
    }


    @Bean
    public Executor threadPoolTaskExecutor() {
        //获取cpu核心数获取核心线程数
        int cupCoreCount = Runtime.getRuntime().availableProcessors();
        int threadCount = cupCoreCount * 2 + 1;
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(threadCount);
        threadPoolTaskExecutor.setMaxPoolSize(threadCount);
        threadPoolTaskExecutor.setKeepAliveSeconds(0);
        threadPoolTaskExecutor.setQueueCapacity(300);
        // 线程前缀名称
        threadPoolTaskExecutor.setThreadNamePrefix("sync-tingshu-Executor--");
        // 该方法用来设置 线程池关闭 的时候 等待 所有任务都完成后，再继续 销毁 其他的 Bean，
        // 这样这些 异步任务 的 销毁 就会先于 数据库连接池对象 的销毁。
        threadPoolTaskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        // 任务的等待时间 如果超过这个时间还没有销毁就 强制销毁，以确保应用最后能够被关闭，而不是阻塞住。
        threadPoolTaskExecutor.setAwaitTerminationSeconds(300);
        // 线程不够用时由调用的线程处理该任务
        threadPoolTaskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        return threadPoolTaskExecutor;
    }
}
