package com.ukora.tradestudent.services

import com.fasterxml.jackson.databind.util.ISO8601DateFormat
import com.mongodb.BasicDBObject
import com.mongodb.BasicDBObjectBuilder
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
import com.ukora.tradestudent.entities.Normalized
import com.ukora.tradestudent.entities.Statuses
import com.ukora.tradestudent.entities.Twitter
import groovy.util.logging.Log4j2
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Meta
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import javax.xml.bind.DatatypeConverter
import java.text.SimpleDateFormat

@Log4j2
@Service
class CaptureAssociationsService {

    final static String GREATER_THAN = "\$gte"
    final static String LESS_THAN = "\$lte"

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

        Memory matchingMemory
        try {
            matchingMemory = getMemory(lesson.date)
        } catch(Exception e){
            e.printStackTrace()
        }
        Twitter matchingTwitter
        try {
            matchingTwitter = getTwitter(lesson.date)
        } catch(Exception e){
            e.printStackTrace()
        }

        obj["processing"] = false
        obj["processed"] = true
        lessons.save(obj)
    }

    Twitter getTwitter(Date twitterDate){

        Twitter the_twitter = new Twitter()

        BasicDBObject query = new BasicDBObject()

        Calendar calendar = Calendar.instance
        calendar.setTime(twitterDate)
        calendar.add(Calendar.SECOND, -45)
        Date fromDate = calendar.time
        calendar.add(Calendar.SECOND, 45)
        Date toDate = calendar.time
        query.put("metadata.datetime", BasicDBObjectBuilder.start(GREATER_THAN, fromDate).add(LESS_THAN, toDate).get())

        DBObject obj = twitter.findOne(query)

        Statuses statuses = new Statuses()
        statuses.text = obj['statuses']['text'] as String

        Metadata metadata = new Metadata()
        metadata.datetime = DatatypeConverter.parseDateTime(obj["metadata"]["datetime"] as String).getTime()
        metadata.hostname = obj["metadata"]["hostname"] as String

        the_twitter.metadata = metadata
        the_twitter.statuses = statuses

        return the_twitter


    }

    Memory getMemory(Date memoryDate){

        Memory the_memory = new Memory()

        BasicDBObject query = new BasicDBObject()

        Calendar calendar = Calendar.instance
        calendar.setTime(memoryDate)
        calendar.add(Calendar.SECOND, -45)
        Date fromDate = calendar.time
        calendar.add(Calendar.SECOND, 45)
        Date toDate = calendar.time
        query.put("metadata.datetime", BasicDBObjectBuilder.start(GREATER_THAN, fromDate).add(LESS_THAN, toDate).get())

        DBObject obj = memory.findOne(query)

        Ask ask = new Ask()
        ask.ask_medium_median_delta = obj['ask']['ask_medium_median_delta'] as Double
        ask.volume_ask_quantity = obj['ask']['volume_ask_quantity'] as Double
        ask.minimum_ask_price = obj['ask']['minimum_ask_price'] as Double
        ask.medium_ask_price = obj['ask']['medium_ask_price'] as Double
        ask.total_ask_value = obj['ask']['total_ask_value'] as Double
        ask.maximum_ask_price = obj['ask']['maximum_ask_price'] as Double
        ask.medium_per_unit_ask_price = obj['ask']['medium_per_unit_ask_price'] as Double
        ask.median_ask_price = obj['ask']['median_ask_price'] as Double

        Bid bid = new Bid()
        bid.volume_bid_quantity = obj['bid']['volume_bid_quantity'] as Double
        bid.bid_medium_median_delta = obj['bid']['bid_medium_median_delta'] as Double
        bid.minimum_bid_price = obj['bid']['minimum_bid_price'] as Double
        bid.median_bid_price = obj['bid']['median_bid_price'] as Double
        bid.maximum_bid_price = obj['bid']['maximum_bid_price'] as Double
        bid.total_bid_value = obj['bid']['total_bid_value'] as Double
        bid.medium_bid_price = obj['bid']['medium_bid_price'] as Double
        bid.medium_per_unit_bid_price = obj['bid']['medium_per_unit_bid_price'] as Double

        Normalized normalized = new Normalized()
        normalized.normalized_spread_bid_price = obj['normalized']['normalized_spread_bid_price'] as Double
        normalized.normalized_medium_per_unit_bid_price = obj['normalized']['normalized_medium_per_unit_bid_price'] as Double
        normalized.normalized_volume_ask_price = obj['normalized']['normalized_volume_ask_price'] as Double
        normalized.normalized_maximum_ask_price = obj['normalized']['normalized_maximum_ask_price'] as Double
        normalized.normalized_bid_medium_median_delta = obj['normalized']['normalized_bid_medium_median_delta'] as Double
        normalized.normalized_minimum_bid_price = obj['normalized']['normalized_minimum_bid_price'] as Double
        normalized.normalized_volume_ask_quantity = obj['normalized']['normalized_volume_ask_quantity'] as Double
        normalized.normalized_medium_bid_price = obj['normalized']['normalized_medium_bid_price'] as Double
        normalized.normalized_spread = obj['normalized']['normalized_spread'] as Double
        normalized.normalized_median_bid_price = obj['normalized']['normalized_median_bid_price'] as Double
        normalized.normalized_spread_ask_price = obj['normalized']['normalized_spread_ask_price'] as Double
        normalized.normalized_maximum_bid_price = obj['normalized']['normalized_maximum_bid_price'] as Double
        normalized.normalized_volume_bid_quantity = obj['normalized']['normalized_volume_bid_quantity'] as Double
        normalized.normalized_ask_medium_median_delta = obj['normalized']['normalized_ask_medium_median_delta'] as Double
        normalized.normalized_medium_ask_price = obj['normalized']['normalized_medium_ask_price'] as Double
        normalized.normalized_medium_per_unit_ask_price = obj['normalized']['normalized_medium_per_unit_ask_price'] as Double
        normalized.normalized_minimum_ask_price = obj['normalized']['normalized_minimum_ask_price'] as Double
        normalized.normalized_median_ask_price = obj['normalized']['normalized_median_ask_price'] as Double
        normalized.normalized_total_bid_value = obj['normalized']['normalized_total_bid_value'] as Double
        normalized.normalized_total_ask_value = obj['normalized']['normalized_total_ask_value'] as Double
        normalized.normalized_volume_bid_price = obj['normalized']['normalized_volume_bid_price'] as Double

        Details details = new Details()
        details.tradecurrency = obj['exchange']['details']['tradecurrency'] as String
        details.pricecurrency = obj['exchange']['details']['pricecurrency'] as String

        Exchange exchange = new Exchange()
        exchange.platform = obj['exchange']['platform'] as String
        exchange.details = details

        Metadata metadata = new Metadata()
        SimpleDateFormat dateParser = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH)
        metadata.datetime = dateParser.parse(obj["metadata"]["datetime"] as String)
        metadata.hostname = obj["metadata"]["hostname"] as String

        Graph graph = new Graph()
        graph.price = obj['graph']['price'] as Double
        graph.quantity = obj['graph']['quantity'] as Double

        the_memory.ask = ask
        the_memory.bid = bid
        the_memory.normalized = normalized
        the_memory.exchange = exchange
        the_memory.metadata = metadata
        the_memory.graph = graph

        return the_memory

    }

}
