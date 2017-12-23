package com.ukora.tradestudent.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
class TaskExecutorConfig {

    @Bean
    ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor()
        pool.setCorePoolSize(5)
        pool.setMaxPoolSize(10)
        pool.setWaitForTasksToCompleteOnShutdown(true)
        return pool
    }

}
