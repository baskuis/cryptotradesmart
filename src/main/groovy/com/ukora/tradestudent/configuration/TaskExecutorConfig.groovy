package com.ukora.tradestudent.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
class TaskExecutorConfig {

    final static Integer POOL_SIZE = 10
    final static Integer MAX_POOL_SIZE = 20

    @Bean
    ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor()
        pool.setCorePoolSize(POOL_SIZE)
        pool.setMaxPoolSize(MAX_POOL_SIZE)
        pool.setWaitForTasksToCompleteOnShutdown(true)
        return pool
    }

}