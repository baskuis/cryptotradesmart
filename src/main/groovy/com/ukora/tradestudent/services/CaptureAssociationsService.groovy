package com.ukora.tradestudent.services

import com.fasterxml.jackson.databind.util.ISO8601DateFormat
import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.mongodb.DBCursor
import com.mongodb.DBObject
import com.ukora.tradestudent.entities.Ask
import com.ukora.tradestudent.entities.Bid
import com.ukora.tradestudent.entities.Details
import com.ukora.tradestudent.entities.Exchange
import com.ukora.tradestudent.entities.Graph
import com.ukora.tradestudent.entities.Lesson
import com.ukora.tradestudent.entities.Memory
import com.ukora.tradestudent.entities.Metadata
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

        Memory matchingMemory = getMemory(lesson.date)
        println matchingMemory







        obj["processing"] = false
        obj["processed"] = true
        lessons.save(obj)
    }

    Memory getMemory(Date memoryDate){

        Memory the_memory = new Memory()

        BasicDBObject query = new BasicDBObject()
        query.put("metadata.datetime", memoryDate)
        DBObject obj = memory.findOne(/*query*/)

        Ask ask = new Ask()
        ask.ask_medium_median_delta = obj['ask']['ask_medium_median_delta'] as long
        ask.volume_ask_quantity = obj['ask']['volume_ask_quantity'] as long
        ask.minimum_ask_price = obj['ask']['minimum_ask_price'] as long
        ask.medium_ask_price = obj['ask']['medium_ask_price'] as long
        ask.total_ask_value = obj['ask']['total_ask_value'] as long
        ask.maximum_ask_price = obj['ask']['maximum_ask_price'] as long
        ask.medium_per_unit_ask_price = obj['ask']['medium_per_unit_ask_price'] as long

        Bid bid = new Bid()
        bid.volume_bid_quantity = obj['bid']['volume_bid_quantity'] as long
        bid.bid_medium_median_delta = obj['bid']['bid_medium_median_delta'] as long
        bid.minimum_bid_price = obj['bid']['minimum_bid_price'] as long
        bid.median_bid_price = obj['bid']['median_bid_price'] as long
        bid.maximum_bid_price = obj['bid']['maximum_bid_price'] as long
        bid.total_bid_value = obj['bid']['total_bid_value'] as long
        bid.medium_bid_price = obj['bid']['medium_bid_price'] as long
        bid.medium_per_unit_bid_price = obj['bid']['medium_per_unit_bid_price'] as long

        Details details = new Details()
        details.tradecurrency = obj['exchange']['details']['tradecurrency'] as String
        details.pricecurrency = obj['exchange']['details']['pricecurrency'] as String

        Exchange exchange = new Exchange()
        exchange.platform = obj['exchange']['platform'] as String
        exchange.details = details

        Metadata metadata = new Metadata()
        Graph graph = new Graph()
        graph.price = obj['graph']['price'] as long
        graph.quantity = obj['graph']['quantity'] as long

        the_memory.ask = ask
        the_memory.bid = bid
        the_memory.exchange = exchange
        the_memory.metadata = metadata
        the_memory.graph = graph

        return the_memory

    }

}
