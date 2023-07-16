package com.upc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
// 线程池的配置
// 在调用asyncExecutor()方法时，
// 将返回一个线程池实现创建的线程池bean。
// 可以使用@Async注解来调用异步方法，然后会将这些方法交给“asyncExecutor”线程池来执行
@Configuration
@EnableAsync //开启了异步调用支持
public class ThreadPoolConfig {

    private static final int CORE_POOL_SIZE = 10;   //核心线程数。即线程池中最少的线程数，即使线程处于空闲状态也不会被销毁
    private static final int MAX_POOL_SIZE = 50;    //最大线程数。当核心线程数已满时，线程池会根据任务的数量动态创建新的线程，最多不超过最大线程数
    private static final int QUEUE_CAPACITY = 100;
    //线程池的任务队列容量。当线程池中的线程已经达到最大线程数时，
    // 如果队列没有满，则将任务放入队列中等待执行；如果队列已满，则会创建新的线程，最多不超过最大线程数
    private static final String THREAD_NAME_PREFIX = "async-";  //便地区分线程池中不同的线程
    private static final int KEEP_ALIVE_SECONDS = 300;          //线程的空闲时间。当线程空闲时间超过这个时间时，线程会被销毁，以释放系统资源。

    @Bean(name = "asyncExecutor")
    public ThreadPoolTaskExecutor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }


//    @Scheduled(fixedDelay = 5000) // 每隔5秒执行一次
//    public void checkExecutor() {
//        //executor == null即未初始化
//        if (executor != null && executor.getThreadPoolExecutor().isShutdown()) {
//            System.out.println("线程池被关闭，重新创建...");
//            executor.initialize();
//        }
//    }

}

