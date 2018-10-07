package com.ukora.tradestudent.configuration

import com.mongodb.Mongo
import com.mongodb.MongoClient
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.AbstractMongoConfiguration

@Configuration
class MongoConfig extends AbstractMongoConfiguration {

    @Override
    protected String getDatabaseName() {
        "tradestudent"
    }

    @Override
    Mongo mongo() throws Exception {
        new MongoClient("127.0.0.1", 27017)
    }

    @Override
    protected String getMappingBasePackage() {
        "com.ukora.tradestudent"
    }

}
