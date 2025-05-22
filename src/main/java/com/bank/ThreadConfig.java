package com.bank;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executors;

@Configuration
public class ThreadConfig implements AsyncConfigurer {
    @Bean(name = "taskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(0);
        executor.setMaxPoolSize(Integer.MAX_VALUE);
        executor.setThreadFactory(Executors.defaultThreadFactory());
        executor.setTaskDecorator(runnable -> {
            return () -> {
                try{
                    Thread.ofVirtual().start(runnable).join();
                } catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            };
        });

        return executor;
    }

    @Override
    public AsyncTaskExecutor getAsyncExecutor() {
        return (AsyncTaskExecutor) taskExecutor();
    }
}

