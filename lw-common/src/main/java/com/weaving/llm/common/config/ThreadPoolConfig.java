package com.weaving.llm.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Author: 35038
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * RAG 流式问答专用线程池
     */
    @Bean(name = "ragChatExecutor")
    public Executor ragChatExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数（根据 CPU 核心数调整）
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());

        // 最大线程数
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);

        // 队列容量（缓冲请求数）
        executor.setQueueCapacity(200);

        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);

        // 线程名称前缀（便于监控）
        executor.setThreadNamePrefix("rag-chat-");

        // 拒绝策略：调用者运行（让主线程处理，避免任务丢失）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }

    /**
     * 通用异步任务线程池
     */
    @Bean(name = "commonExecutor")
    public Executor commonExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("common-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * IO 密集型任务线程池（适合你的 RAG 场景，因为涉及网络调用）
     */
    @Bean(name = "ioIntensiveExecutor")
    public Executor ioIntensiveExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // IO 密集型任务，线程数可以设置大一些
        // 公式：线程数 = CPU 核心数 * (1 + IO等待时间/CPU计算时间)
        int cpuCores = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(cpuCores * 2);
        executor.setMaxPoolSize(cpuCores * 4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("io-rag-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
