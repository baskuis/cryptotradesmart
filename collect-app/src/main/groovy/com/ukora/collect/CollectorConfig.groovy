package com.ukora.collect

import com.mongodb.MongoClient
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.AbstractMongoConfiguration

@Configuration
class CollectorConfig extends AbstractMongoConfiguration {

    @Override
    protected String getDatabaseName() {
        "tradestudent"
    }

    @Override
    MongoClient mongoClient() {
        return new MongoClient("127.0.0.1", 27017)
    }
    
}