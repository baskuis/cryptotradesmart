package com.ukora.tradestudent.configuration


import com.mongodb.MongoClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.AbstractMongoConfiguration

@Configuration
class MongoConfig extends AbstractMongoConfiguration {

    @Override
    protected String getDatabaseName() {
        "tradestudent"
    }

    @Override
    protected String getMappingBasePackage() {
        "com.ukora.tradestudent"
    }

    @Bean
    @Override
    MongoClient mongoClient() {
        return new MongoClient("127.0.0.1", 27017)
    }

}