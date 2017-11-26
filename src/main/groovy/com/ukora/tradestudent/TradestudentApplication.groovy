package com.ukora.tradestudent

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableScheduling

@EnableCaching
@EnableScheduling
@SpringBootApplication(exclude=[
		SecurityAutoConfiguration.class])
@EnableAutoConfiguration(exclude=[
		DataSourceAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class])
class TradestudentApplication {

	static void main(String[] args) {
		println new Date()
		SpringApplication.run TradestudentApplication, args
	}

}