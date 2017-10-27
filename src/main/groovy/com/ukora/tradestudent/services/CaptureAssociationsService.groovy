package com.ukora.tradestudent.services

import com.fasterxml.jackson.databind.util.ISO8601DateFormat
import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBCursor
import com.mongodb.DBObject
import com.ukora.tradestudent.entities.Exchange
import com.ukora.tradestudent.entities.Lesson
import groovy.util.logging.Log4j2
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import javax.xml.bind.DatatypeConverter

@Log4j2
@Service
class CaptureAssociationsService {

    @Autowired
    MongoTemplate mongoTemplate

    DBCollection lessons
    DBCollection memory
    DBCollection news
    DBCollection twitter
    DBCollection associations

    @PostConstruct
    void postConstruct(){
        lessons = mongoTemplate.getCollection("lessons")
        memory = mongoTemplate.getCollection("memory")
        news = mongoTemplate.getCollection("news")
        twitter = mongoTemplate.getCollection("twitter")
        associations = mongoTemplate.getCollection("associations")
    }

    @Scheduled(cron = "* * * * * *")
    learn(){
        DBObject obj = lessons.findOne()
        obj["processing"] = true
        lessons.save(obj)
        Lesson lesson = new Lesson()
        lesson.setId(obj["_id"] as String)
        lesson.setDate(DatatypeConverter.parseDateTime(obj["date"] as String).getTime())
        lesson.setPrice(obj["price"] as long)
        Exchange exchange = new Exchange()
        exchange.setPlatform(obj["exchange"]["platform"] as String)
        lesson.setExchange(exchange)
        Thread.sleep(500)
        log.info('Running scheduled retrieve lesson task')
        obj["processing"] = false
        obj["processed"] = true
        lessons.save(obj)
    }

}
