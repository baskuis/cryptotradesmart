package com.ukora.tradestudent

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@EnableMongoRepositories
@SpringBootApplication(exclude = [
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
])
class TradestudentApplication {

    public static boolean DEBUG_LOGGING_ENABLED = false

    public static boolean CONSIDER_TWITTER = false

    public static boolean CONSIDER_NEWS = false

    static void main(String[] args) {
        println new Date()
        SpringApplication.run TradestudentApplication, args
    }

}