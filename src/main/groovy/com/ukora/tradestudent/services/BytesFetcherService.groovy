package com.ukora.tradestudent.services

import com.mongodb.*
import com.ukora.tradestudent.bayes.numbers.NumberAssociation
import com.ukora.tradestudent.entities.*
import com.ukora.tradestudent.tags.BuyTag
import com.ukora.tradestudent.tags.SellTag
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import javax.xml.bind.DatatypeConverter
import java.text.SimpleDateFormat

@Service
class BytesFetcherService {

    final static String GREATER_THAN = "\$gte"
    final static String LESS_THAN = "\$lte"
    final static String NOT_EQUALS = "\$ne"

    SimpleDateFormat dateParser = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH)

    @Autowired
    MongoTemplate mongoTemplate

    DBCollection lessons
    DBCollection memory
    DBCollection news
    DBCollection twitter
    DBCollection associations
    DBCollection brain

    @PostConstruct
    void postConstruct(){
        lessons = mongoTemplate.getCollection("lessons")
        memory = mongoTemplate.getCollection("memory")
        news = mongoTemplate.getCollection("news")
        twitter = mongoTemplate.getCollection("twitter")
        associations = mongoTemplate.getCollection("associations")
        brain = mongoTemplate.getCollection("brain")
    }

    /**
     * Flush brain collection
     *
     */
    void wiskyBender(){
        brain.remove(new BasicDBObject())
    }

    /**
     * Return existing or new number association object
     *
     * @param reference
     * @return
     */
    NumberAssociation getNumberAssociation(String reference){
        BasicDBObject query = new BasicDBObject()
        query.put('reference', reference)
        DBObject obj = brain.findOne(query)
        if(obj == null) return new NumberAssociation(
                id: null,
                reference: reference,
                mean: 0 as Double,
                count: 0 as Integer,
                standard_deviation: 0 as Double
        )
        return new NumberAssociation(
                id: obj['_id'] as String,
                reference: obj['reference'] as String,
                mean: obj['mean'] as Double,
                count: obj['count'] as Integer,
                standard_deviation: obj['standard_deviation'] as Double
        )
    }

    /**
     * Save a number association
     *
     * @param numberAssociation
     */
    void saveNumberAssociation(NumberAssociation numberAssociation){
        DBObject obj = new BasicDBObject()
        if(numberAssociation.id){
            obj['_id'] = new ObjectId(numberAssociation.id)
        }
        obj['reference'] = numberAssociation.reference as String
        obj['mean'] = numberAssociation.mean as String
        obj['standard_deviation'] = numberAssociation.standard_deviation as String
        obj['count'] = numberAssociation.count as String
        brain.save(obj)
    }

    /**
     * Update lessons - mark them to be up for processing once more
     *
     */
    void resetLessons(){
        DBCursor cursor = lessons.find()
        while(cursor.hasNext()){
            DBObject obj = cursor.next()
            obj.removeField('processed')
            lessons.save(obj)
        }
    }

    /**
     * Get the next un-processed lesson
     *
     * @return
     */
    Lesson getNextLesson(){
        BasicDBObject query = new BasicDBObject()
        query.put("processed", BasicDBObjectBuilder.start(NOT_EQUALS, true).get())
        DBObject obj = lessons.findOne(query)
        if(obj != null) {
            obj["processed"] = true
            lessons.save(obj)
            Lesson lesson = new Lesson()
            switch (obj['tag']) {
                case 'buy':
                    lesson.tag = new BuyTag()
                    break
                case 'sell':
                    lesson.tag = new SellTag()
                    break
            }
            lesson.setId(obj["_id"] as String)
            lesson.setDate(DatatypeConverter.parseDateTime(obj["date"] as String).getTime())
            lesson.setPrice(obj["price"] as long)
            Exchange exchange = new Exchange()
            exchange.setPlatform(obj["exchange"]["platform"] as String)
            lesson.setExchange(exchange)
            lesson.setDbObject(obj)
            return lesson
        }
        return null
    }

    /**
     * Get relevant news snapshot
     *
     * @param newsDate
     * @return
     */
    List<News> getNews(Date newsDate){
        BasicDBObject query = new BasicDBObject()
        Calendar calendar = Calendar.instance
        calendar.setTime(newsDate)
        calendar.add(Calendar.SECOND, -45)
        Date fromDate = calendar.time
        calendar.add(Calendar.SECOND, 90)
        Date toDate = calendar.time
        query.put("metadata.datetime", BasicDBObjectBuilder
                .start(GREATER_THAN, fromDate)
                .add(LESS_THAN, toDate).get())
        DBCursor cursor = news.find(query)
        List<News> response = []
        while(cursor.hasNext()){
            DBObject obj = cursor.next()
            News theNews = new News()
            Article theArticle = new Article()
            theArticle.title = obj['article']['title']
            theArticle.summary = obj['article']['summary']
            theArticle.published = dateParser.parse(obj["article"]['published'] as String)
            Metadata metadata = new Metadata()
            metadata.hostname = obj["metadata"]['hostname']
            metadata.datetime = dateParser.parse(obj["metadata"]['datetime'] as String)
            theNews.metadata = metadata
            theNews.article = theArticle
            response << theNews
        }
        return response
    }

    /**
     * Get relevant twitter chatter
     *
     * @param twitterDate
     * @return
     */
    Twitter getTwitter(Date twitterDate){
        Twitter the_twitter = new Twitter()
        BasicDBObject query = new BasicDBObject()
        Calendar calendar = Calendar.instance
        calendar.setTime(twitterDate)
        calendar.add(Calendar.SECOND, -45)
        Date fromDate = calendar.time
        calendar.add(Calendar.SECOND, 90)
        Date toDate = calendar.time
        query.put("metadata.datetime", BasicDBObjectBuilder
                .start(GREATER_THAN, fromDate)
                .add(LESS_THAN, toDate).get())
        DBObject obj = twitter.findOne(query)
        Statuses statuses = new Statuses()
        statuses.text = obj['statuses']['text'] as String
        Metadata metadata = new Metadata()
        metadata.datetime = dateParser.parse(obj["metadata"]["datetime"] as String)
        metadata.hostname = obj["metadata"]["hostname"] as String
        the_twitter.metadata = metadata
        the_twitter.statuses = statuses
        return the_twitter
    }

    /**
     * Get marketplace snapshot
     * from memory collection
     *
     * @param memoryDate
     * @return
     */
    Memory getMemory(Date memoryDate){
        Memory the_memory = new Memory()
        BasicDBObject query = new BasicDBObject()
        Calendar calendar = Calendar.instance
        calendar.setTime(memoryDate)
        calendar.add(Calendar.SECOND, -45)
        Date fromDate = calendar.time
        calendar.add(Calendar.SECOND, 90)
        Date toDate = calendar.time
        query.put("metadata.datetime", BasicDBObjectBuilder
                .start(GREATER_THAN, fromDate)
                .add(LESS_THAN, toDate).get())
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
        exchange.exchange = obj['exchange']['exchange'] as String
        exchange.details = details
        Metadata metadata = new Metadata()
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
