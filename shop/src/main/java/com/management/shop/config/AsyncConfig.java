package com.management.shop.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@Configuration
public class AsyncConfig {

	@Bean("mailAsync")
	public Executor asyncTaskExecutor() {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setMaxPoolSize(40);
		ex.setCorePoolSize(30);
		ex.setQueueCapacity(200);
        ex.setThreadNamePrefix("MailAsync-");
		ex.initialize();
		return ex;

	}
    @Bean("geminiAsync")
    public Executor geminiAsyncTaskExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setMaxPoolSize(40);
        ex.setCorePoolSize(30);
        ex.setQueueCapacity(200);
        ex.setThreadNamePrefix("GeminiAsync-");
        ex.initialize();
        return ex;
    }

}
