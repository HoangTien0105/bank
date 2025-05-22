package com.bank;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executors;

@Configuration
public class ThreadConfig {
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
}
