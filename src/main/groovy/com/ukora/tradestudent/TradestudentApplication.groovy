package com.ukora.tradestudent

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
@EnableAutoConfiguration(exclude=[
		DataSourceAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class])
class TradestudentApplication {

	static void main(String[] args) {
		SpringApplication.run TradestudentApplication, args
	}

}