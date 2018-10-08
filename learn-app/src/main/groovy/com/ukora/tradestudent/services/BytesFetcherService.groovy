package com.ukora.tradestudent.services

import com.mongodb.*
import com.ukora.tradestudent.TradestudentApplication
import com.ukora.tradestudent.bayes.numbers.NumberAssociation
import com.ukora.tradestudent.entities.*
import com.ukora.tradestudent.repositories.LessonRepository
import com.ukora.tradestudent.repositories.PropertyRepository
import com.ukora.tradestudent.repositories.SimulationResultRepository
import com.ukora.tradestudent.strategy.trading.TradeExecution
import com.ukora.tradestudent.tags.TagGroup
import com.ukora.tradestudent.utils.Logger
import com.ukora.tradestudent.utils.NerdUtils
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils

import javax.annotation.PostConstruct
import javax.xml.bind.DatatypeConverter
import java.text.SimpleDateFormat

@Service
class BytesFetcherService {

    final static String GREATER_THAN = "\$gte"
    final static String LESS_THAN = "\$lte"
    final static String NOT_EQUALS = "\$ne"

    SimpleDateFormat dateParser = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH)

    final static String COLLECTION_LESSONS = "lessons"
    final static String COLLECTION_MEMORY = "memory"
    final static String COLLECTION_NEWS = "news"
    final static String COLLECTION_TWITTER = "twitter"
    final static String COLLECTION_ASSOCIATIONS = "associations"
    final static String COLLECTION_BRAIN = "brain"
    final static String COLLECTION_BRAIN_COUNT = "brainCount"
    final static String COLLECTION_SIMULATIONS = "simulations"
    final static String COLLECTION_PROPERTIES = "properties"
    final static String COLLECTION_SIMULATED_TRADES = "simulatedTrades"

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    TagService tagService

    Map<String, TagGroup> tagGroupMap

    @Autowired
    MongoClient mongoClient

    @Autowired
    PropertyRepository propertyRepository
    @Autowired
    LessonRepository lessonRepository
    @Autowired
    SimulationResultRepository simulationResultRepository

    MongoOperations lessons
    MongoOperations memory
    MongoOperations news
    MongoOperations twitter
    MongoOperations associations
    MongoOperations brain
    MongoOperations brainCount
    MongoOperations simulations
    MongoOperations properties
    MongoOperations simulatedTrades

    @PostConstruct
    void init() {
        this.lessons = new MongoTemplate(mongoClient, COLLECTION_LESSONS)
        this.memory = new MongoTemplate(mongoClient, COLLECTION_MEMORY)
        this.news = new MongoTemplate(mongoClient, COLLECTION_NEWS)
        this.twitter = new MongoTemplate(mongoClient, COLLECTION_TWITTER)
        this.associations = new MongoTemplate(mongoClient, COLLECTION_ASSOCIATIONS)
        this.brain = new MongoTemplate(mongoClient, COLLECTION_BRAIN)
        this.brainCount = new MongoTemplate(mongoClient, COLLECTION_BRAIN_COUNT)
        this.simulations = new MongoTemplate(mongoClient, COLLECTION_SIMULATIONS)
        this.properties = new MongoTemplate(mongoClient, COLLECTION_PROPERTIES)
        this.simulatedTrades = new MongoTemplate(mongoClient, COLLECTION_SIMULATED_TRADES)
        tagGroupMap = applicationContext.getBeansOfType(TagGroup)
    }

    /**
     * Get property
     *
     * @param name
     * @return
     */
    @Cacheable("properties")
    Property getProperty(String name) {
        return propertyRepository.findByName(name)
    }

    /**
     * Save property
     *
     * @param property
     */
    @CacheEvict(value = "properties", allEntries = true)
    void saveProperty(Property property) {
        propertyRepository.save(property)
    }

    /**
     * Save property
     *
     * @param name
     * @param value
     */
    @CacheEvict(value = "properties", allEntries = true)
    void saveProperty(String name, String value) {
        propertyRepository.save(new Property(
                name: name,
                value: value
        ))
    }

    /**
     * Get latest simulated trade entries
     *
     * @return
     */
    List<SimulatedTradeEntry> getLatestSimulatedTradeEntries() {
        List<SimulatedTradeEntry> entries = []
        BasicDBObject sortByDateDesc = new BasicDBObject()
        sortByDateDesc.put('date', -1)
        DBCursor cursor = this.simulatedTrades.find().sort(sortByDateDesc).limit(2000)

        simulationResultRepository.findAll(new Sort())

        while (cursor.hasNext()) {
            DBObject obj = cursor.next()
            Metadata metadata = new Metadata(
                    hostname: obj?.metadata?.hostname as String,
                    datetime: new Date(obj?.metadata?.datetime as String)
            )
            entries << new SimulatedTradeEntry(
                    metadata: metadata,
                    tradeType: Enum.valueOf(TradeExecution.TradeType.class, obj['tradeType'] as String),
                    date: new Date(obj['date'] as String),
                    amount: obj['amount'] as Double,
                    price: obj['price'] as Double,
                    balanceA: obj['balanceA'] as Double,
                    balanceB: obj['balanceB'] as Double,
                    totalValueA: obj['totalValueA'] as Double
            )
        }
        return entries.sort({ SimulatedTradeEntry a, SimulatedTradeEntry b ->
            a.getDate() <=> b.getDate()
        })
    }

    /**
     * Get latest simulated trade entry
     *
     * @return
     */
    SimulatedTradeEntry getLatestSimulatedTradeEntry() {
        BasicDBObject sortByDateDesc = new BasicDBObject()
        sortByDateDesc.put('date', -1)
        DBCursor cursor = this.simulatedTrades.find().sort(sortByDateDesc).limit(1)
        while (cursor.hasNext()) {
            DBObject obj = cursor.next()
            Metadata metadata = new Metadata(
                    hostname: obj?.metadata?.hostname as String,
                    datetime: new Date(obj?.metadata?.datetime as String)
            )
            return new SimulatedTradeEntry(
                    metadata: metadata,
                    tradeType: Enum.valueOf(TradeExecution.TradeType.class, obj['tradeType'] as String),
                    date: new Date(obj['date'] as String),
                    amount: obj['amount'] as Double,
                    price: obj['price'] as Double,
                    balanceA: obj['balanceA'] as Double,
                    balanceB: obj['balanceB'] as Double,
                    totalValueA: obj['totalValueA'] as Double
            )
        }
        return null
    }

    /**
     * Insert simulation trade entry
     *
     * @param simulatedTradeEntry
     */
    void insertSimulatedTradeEntry(SimulatedTradeEntry simulatedTradeEntry) {
        if (!simulatedTradeEntry) return
        DBObject obj = new BasicDBObject()
        obj['tradeType'] = simulatedTradeEntry.getTradeType() as String
        obj['date'] = simulatedTradeEntry.getDate()
        obj['amount'] = simulatedTradeEntry.getAmount()
        obj['price'] = simulatedTradeEntry.getPrice()
        obj['balanceA'] = simulatedTradeEntry.getBalanceA()
        obj['balanceB'] = simulatedTradeEntry.getBalanceB()
        obj['totalValueA'] = simulatedTradeEntry.getTotalValueA()
        obj.get('metadata', [:])['datetime'] = simulatedTradeEntry.metadata?.datetime
        obj.get('metadata', [:])['hostname'] = simulatedTradeEntry.metadata?.hostname
        this.simulatedTrades.insert(obj)
    }

    /**
     * Flush brain collection
     *
     */
    @CacheEvict(value = "brainNodes", allEntries = true)
    void whiskeyBender() {
        this.brain.remove(new BasicDBObject())
    }

    /**
     * Set ceiling on brain nodes
     *
     * @param tagGroup
     * @param maxCount
     */
    @CacheEvict(value = "brainNodes", allEntries = true)
    void resetBrainNodesCount(TagGroup tagGroup, int maxCount) {
        DBCursor cursor = this.brain.find()
        while (cursor.hasNext()) {
            DBObject obj = cursor.next()
            if (
            obj['tag'] &&
                    tagGroup.tags().collect({ it.tagName }).contains(obj['tag']) &&
                    obj['count'] && obj['count'] > maxCount
            ) {
                obj['count'] = maxCount
                this.brain.save(obj)
            }
        }
    }

    /**
     * Evict all entries from cache
     *
     */
    @CacheEvict(value = [
            "news",
            "twitter",
            "memory",
            "brainNodes",
            "simulations",
            "properties",
            "associations",
            "brainCount"
    ], allEntries = true)
    void flushCache() {}

    /**
     * Get all simulations
     *
     * @return
     */
    @Cacheable("simulations")
    List<SimulationResult> getSimulations() {
        List<SimulationResult> theSimulations = []
        DBCursor cursor = simulations.find()
        while (cursor.hasNext()) {
            try {
                DBObject obj = cursor.next()
                SimulationResult simulation = new SimulationResult()
                if (obj['_id']) {
                    simulation.id = obj['_id']
                }
                simulation.differential = obj['differential'] as Double
                simulation.startDate = dateParser.parse(obj['startDate'] as String)
                simulation.probabilityCombinerStrategy = obj['probabilityCombinerStrategy']
                simulation.tradeExecutionStrategy = obj['tradeExecutionStrategy']
                simulation.tradeIncrement = obj['tradeIncrement'] as Double
                simulation.buyThreshold = obj['buyThreshold'] as Double
                simulation.sellThreshold = obj['sellThreshold'] as Double
                simulation.tradeCount = obj['tradeCount'] as Integer
                simulation.totalValue = obj['totalValue'] as Double
                simulation.endDate = dateParser.parse(obj['endDate'] as String)
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
                simulation.exchange = exchange
                simulation.metadata = metadata
                theSimulations << simulation
            } catch (Exception e) {
                e.printStackTrace()
            }
        }
        return theSimulations
    }

    /**
     * Persist a simulation
     *
     * @param simulation
     */
    @CacheEvict(value = "simulations", allEntries = true)
    void saveSimulation(SimulationResult simulation) {
        try {
            DBObject obj = new BasicDBObject()
            if (simulation.id) {
                obj['_id'] = new ObjectId(simulation.id)
            }
            obj['startDate'] = simulation.startDate
            obj['endDate'] = simulation.endDate
            obj['differential'] = String.format("%.16f", simulation.differential)
            obj['probabilityCombinerStrategy'] = simulation.probabilityCombinerStrategy
            obj['tradeExecutionStrategy'] = simulation.tradeExecutionStrategy
            obj['tradeIncrement'] = String.format("%.16f", simulation.tradeIncrement)
            obj['buyThreshold'] = String.format("%.16f", simulation.buyThreshold)
            obj['sellThreshold'] = String.format("%.16f", simulation.sellThreshold)
            obj['totalValue'] = String.format("%.16f", simulation.totalValue)
            obj['tradeCount'] = String.format("%d", simulation.tradeCount)
            obj.get('exchange', [:])['platform'] = simulation.exchange?.platform
            obj.get('exchange', [:])['exchange'] = simulation.exchange?.exchange
            (obj.get('exchange', [:]) as Map).get('details', [:])['tradecurrency'] = simulation.exchange?.details?.tradecurrency
            (obj.get('exchange', [:]) as Map).get('details', [:])['pricecurrency'] = simulation.exchange?.details?.pricecurrency
            obj.get('metadata', [:])['datetime'] = simulation.metadata?.datetime
            obj.get('metadata', [:])['hostname'] = simulation.metadata?.hostname
            this.simulations.save(obj)
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

    /**
     * Get brain count
     *
     * @param reference
     * @param tag
     * @param source
     * @return
     */
    @Cacheable("brainCount")
    BrainCount getBrainCount(String reference, String source) {
        BasicDBObject query = new BasicDBObject()
        query.put('reference', reference)
        DBObject obj = this.brainCount.findOne(query)
        if (obj == null) return new BrainCount(
                id: null,
                source: source,
                reference: reference,
                counters: [:]
        )
        return new BrainCount(
                id: obj['_id'] as String,
                source: obj['source'] as String,
                reference: obj['reference'] as String,
                counters: obj['counters'] as Map,
        )
    }

    /**
     * Save brain count
     *
     * @param brainCount
     */
    @CacheEvict(value = "brainCount", allEntries = true)
    void saveBrainCount(BrainCount brainCount) {
        DBObject obj = new BasicDBObject()
        if (brainCount.id) {
            obj['_id'] = new ObjectId(brainCount.id)
        }
        obj['source'] = brainCount.source as String
        obj['reference'] = brainCount.reference as String
        obj['counters'] = brainCount.counters
        this.brainCount.save(obj)
    }

    /**
     * Return existing or new number association object
     *
     * @param reference
     * @return
     */
    Brain getBrain(String reference, String tag) {
        BasicDBObject query = new BasicDBObject()
        query.put('reference', reference)
        query.put('tag', tag)
        DBObject obj = this.brain.findOne(query)
        if (obj == null) return new Brain(
                id: null,
                tag: tag,
                reference: reference,
                mean: 0 as Double,
                count: 0 as Integer,
                standard_deviation: 0 as Double
        )
        return new Brain(
                id: obj['_id'] as String,
                tag: obj['tag'] as String,
                reference: obj['reference'] as String,
                mean: obj['mean'] as Double,
                count: obj['count'] as Integer,
                standard_deviation: obj['standard_deviation'] as Double
        )
    }

    /**
     * Save a number association
     *
     * @param brain
     */
    @CacheEvict(value = [
            "brainNodes",
            "associations"
    ], allEntries = true)
    void saveBrain(Brain brain) {
        DBObject obj = new BasicDBObject()
        if (brain.id) {
            obj['_id'] = new ObjectId(brain.id)
        }
        obj['tag'] = brain.tag as String
        obj['reference'] = brain.reference as String
        obj['mean'] = String.format("%.16f", brain.mean)
        obj['standard_deviation'] = String.format("%.16f", brain.standard_deviation)
        obj['count'] = brain.count
        this.brain.save(obj)
    }

    /**
     * Update lessons - mark them to be up for processing once more
     *
     */
    void resetLessons() {
        lessonRepository.findAll().each {
            it.processed = false
            lessonRepository.save(it)
        }
    }

    /**
     * Return all brain nodes
     *
     * @return
     */
    @Cacheable("brainNodes")
    Map<String, BrainNode> getAllBrainNodes() {
        Map<String, BrainNode> nodes = [:]
        DBCursor cursor = this.brain.find().limit(5000)
        while (cursor.hasNext()) {
            DBObject object = cursor.next()
            nodes.get(object['reference'] as String, new BrainNode(reference: object['reference'])).
                    tagReference.put(object['tag'] as String, new NumberAssociation(
                    tagGroup: tagGroupMap.find { (it.value.tags().find { it.getTagName() == object['tag'] }) }?.key,
                    tag: object['tag'],
                    mean: Double.parseDouble(object['mean'] as String),
                    count: Integer.parseInt(object['count'] as String),
                    standard_deviation: Double.parseDouble(object['standard_deviation'] as String)
            ))
        }
        return nodes
    }

    /**
     * Hydrate lesson - with all the relevant info
     *
     * @param lesson
     * @return
     */
    public <T extends AbstractAssociation> T hydrateAssociation(T someAssociation) {
        if (!someAssociation) return null
        try {
            someAssociation.memory = getMemory(someAssociation.date)
            if (someAssociation.memory == null) {
                Logger.log("empty memory object")
                return null
            }
            someAssociation.exchange = someAssociation.memory.exchange
            someAssociation.price = someAssociation.memory.graph.price
            someAssociation.date = someAssociation.memory.metadata.datetime
        } catch (Exception e) {
            e.printStackTrace()
        }
        if (TradestudentApplication.CONSIDER_TWITTER) {
            try {
                someAssociation.twitter = getTwitter(someAssociation.date)
            } catch (Exception e) {
                e.printStackTrace()
            }
        }
        if (TradestudentApplication.CONSIDER_NEWS) {
            try {
                someAssociation.news = getNews(someAssociation.date)
            } catch (Exception e) {
                e.printStackTrace()
            }
        }
        someAssociation.intervals.each { String key ->
            Calendar calendar = Calendar.instance
            calendar.setTime(someAssociation.date)
            switch (key) {
                case '2minute':
                    calendar.add(Calendar.MINUTE, -2)
                    break
                case '5minute':
                    calendar.add(Calendar.MINUTE, -5)
                    break
                case '10minute':
                    calendar.add(Calendar.MINUTE, -10)
                    break
                case '15minute':
                    calendar.add(Calendar.MINUTE, -15)
                    break
                case '30minute':
                    calendar.add(Calendar.MINUTE, -30)
                    break
                case '45minute':
                    calendar.add(Calendar.MINUTE, -45)
                    break
                case '1hour':
                    calendar.add(Calendar.HOUR, -1)
                    break
                case '2hour':
                    calendar.add(Calendar.HOUR, -2)
                    break
                case '4hour':
                    calendar.add(Calendar.HOUR, -4)
                    break
                case '8hour':
                    calendar.add(Calendar.HOUR, -8)
                    break
                case '16hour':
                    calendar.add(Calendar.HOUR, -16)
                    break
                default:
                    throw new RuntimeException(String.format("Unknown interval %s", key))
            }
            try {
                Memory thisMemory = getMemory(calendar.time)
                someAssociation.previousMemory.put(key, thisMemory)
                try {
                    if (thisMemory?.graph?.price && someAssociation?.price && someAssociation?.price > 0) {
                        Double previousPriceProportion = thisMemory.graph.price / someAssociation.price
                        if (!previousPriceProportion.naN) {
                            someAssociation.previousPrices.put(key, previousPriceProportion)
                        }
                    } else {
                        Logger.debug("missing price cannot set previous price")
                        Logger.debug("thisMemory?.graph?.price: " + thisMemory?.graph?.price)
                        Logger.debug("someAssociation?.price: " + someAssociation?.price)
                    }
                } catch (e) {
                    e.printStackTrace()
                }
            } catch (e) {
                e.printStackTrace()
            }
            try {
                someAssociation.previousNews.put(key, getNews(calendar.time))
            } catch (e) {
                e.printStackTrace()
            }
        }
        return someAssociation
    }

    /**
     * Get lesson count
     *
     * @param tag
     * @return
     */
    Integer getLessonCount(String tag) {
        BasicDBObject query = new BasicDBObject()
        query.put("tag", tag)
        lessons.find(query).count()
    }

    /**
     * Get the next un-processed lesson
     *
     * @return
     */
    Lesson getNextTextLesson() {
        BasicDBObject query = new BasicDBObject()
        query.put("textProcessed", BasicDBObjectBuilder.start(NOT_EQUALS, true).get())
        DBObject obj = lessons.findOne(query)
        if (obj != null) {
            obj["textProcessed"] = true
            lessons.save(obj)
            Lesson lesson = new Lesson()
            def tag = tagService.getTagByName(obj['tag'] as String)
            if (tag) {
                lesson.tag = tag
            } else {
                throw new RuntimeException(String.format('WTF! Tag %s cannot be mapped to a valid tag', obj['tag']))
            }
            lesson.setId(obj["_id"] as String)
            try {
                lesson.setDate(DatatypeConverter.parseDateTime(obj["date"] as String).getTime())
            } catch (IllegalArgumentException e) {
                lesson.setDate(new Date(obj['date'] as String))
            }
            lesson.setPrice(obj["price"] as Double)
            Exchange exchange = new Exchange()
            exchange.setPlatform(obj["exchange"]["platform"] as String)
            lesson.setExchange(exchange)
            lesson.setDbObject(obj)
            return lesson
        }
        return null
    }

    /**
     * Get the next un-processed lesson
     *
     * @return
     */
    Lesson getNextLesson() {
        BasicDBObject query = new BasicDBObject()
        query.put("processed", BasicDBObjectBuilder.start(NOT_EQUALS, true).get())
        DBObject obj = lessons.findOne(query)
        if (obj != null) {
            obj["processed"] = true
            lessons.save(obj)
            Lesson lesson = new Lesson()
            def tag = tagService.getTagByName(obj['tag'] as String)
            if (tag) {
                lesson.tag = tag
            } else {
                throw new RuntimeException(String.format('WTF! Tag %s cannot be mapped to a valid tag', obj['tag']))
            }
            lesson.setId(obj["_id"] as String)
            try {
                lesson.setDate(DatatypeConverter.parseDateTime(obj["date"] as String).getTime())
            } catch (IllegalArgumentException e) {
                lesson.setDate(new Date(obj['date'] as String))
            }
            lesson.setPrice(obj["price"] as Double)
            Exchange exchange = new Exchange()
            exchange.setPlatform(obj["exchange"]["platform"] as String)
            lesson.setExchange(exchange)
            lesson.setDbObject(obj)
            return lesson
        }
        return null
    }

    /**
     * Save lesson
     *
     * @param lesson
     */
    void saveLesson(Lesson lesson) {
        if (!lesson.tag) return
        if (!lesson.date) return
        if (!lesson.price) return
        try {
            DBObject obj = new BasicDBObject()
            if (lesson.id) {
                obj['_id'] = new ObjectId(lesson.id)
            }
            obj["tag"] = lesson.tag.tagName
            obj["price"] = String.format("%.16f", lesson.price)
            obj["date"] = lesson.date
            obj.get("exchange", [:])["platform"] = lesson.exchange?.platform
            obj.get("exchange", [:])["exchange"] = lesson.exchange?.exchange
            this.lessons.save(obj)
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

    /**
     * Get relevant news snapshot
     *
     * @param newsDate
     * @return
     */
    @Cacheable("news")
    List<News> getNews(Date newsDate) {
        BasicDBObject query = new BasicDBObject()
        Calendar calendar = Calendar.instance
        calendar.setTime(newsDate)
        calendar.add(Calendar.SECOND, -45)
        Date fromDate = calendar.time
        calendar.add(Calendar.SECOND, 90)
        Date toDate = calendar.time
        query.put('metadata.datetime', BasicDBObjectBuilder
                .start(GREATER_THAN, fromDate)
                .add(LESS_THAN, toDate).get())
        DBCursor cursor = news.find(query)
        List<News> response = []
        while (cursor.hasNext()) {
            try {
                DBObject obj = cursor.next()
                if (!obj['metadata']['datetime'] || StringUtils.isEmpty(obj['metadata']['datetime'])) continue
                if (!obj['article']['published'] || StringUtils.isEmpty(obj['article']['published'])) continue
                if (!obj['article']['title'] || StringUtils.isEmpty(obj['article']['title'])) continue
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
            } catch (Exception e) {
                /** Ignore */
            }
        }
        return response
    }

    /**
     * Get relevant twitter chatter
     *
     * @param twitterDate
     * @return
     */
    @Cacheable("twitter")
    Twitter getTwitter(Date twitterDate) {
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
        if (!obj) return null
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
    @Cacheable("memory")
    Memory getMemory(Date memoryDate) {
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
        if (!obj) return null
        try {
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
            Date date = NerdUtils.parseString(obj["metadata"]["datetime"] as String)
            metadata.datetime = (date) ? date : memoryDate
            metadata.hostname = obj["metadata"]["hostname"] as String
            Graph graph = new Graph()
            try {
                graph.price = obj['graph']['price'] as Double
                graph.quantity = obj['graph']['quantity'] as Double
            } catch (Exception e) {
                Logger.debug('no graph.price')
            }
            the_memory.graph = graph
            the_memory.ask = ask
            the_memory.bid = bid
            the_memory.normalized = normalized
            the_memory.exchange = exchange
            the_memory.metadata = metadata
            return the_memory
        } catch (Exception e) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Insert memory
     *
     *
     * @param the_memory
     */
    void insertMemory(Memory the_memory) {
        if (!the_memory) return
        try {
            DBObject obj = new BasicDBObject()
            obj['ask'] = [:]
            obj['ask']['ask_medium_median_delta'] = the_memory.ask?.ask_medium_median_delta
            obj['ask']['volume_ask_quantity'] = the_memory.ask?.volume_ask_quantity
            obj['ask']['minimum_ask_price'] = the_memory.ask?.minimum_ask_price
            obj['ask']['medium_ask_price'] = the_memory.ask?.medium_ask_price
            obj['ask']['total_ask_value'] = the_memory.ask?.total_ask_value
            obj['ask']['maximum_ask_price'] = the_memory.ask?.maximum_ask_price
            obj['ask']['medium_per_unit_ask_price'] = the_memory.ask?.medium_per_unit_ask_price
            obj['ask']['median_ask_price'] = the_memory.ask?.median_ask_price
            obj['bid'] = [:]
            obj['bid']['volume_bid_quantity'] = the_memory.bid?.volume_bid_quantity
            obj['bid']['bid_medium_median_delta'] = the_memory.bid?.bid_medium_median_delta
            obj['bid']['minimum_bid_price'] = the_memory.bid?.minimum_bid_price
            obj['bid']['median_bid_price'] = the_memory.bid?.median_bid_price
            obj['bid']['maximum_bid_price'] = the_memory.bid?.maximum_bid_price
            obj['bid']['total_bid_value'] = the_memory.bid?.total_bid_value
            obj['bid']['medium_bid_price'] = the_memory.bid?.medium_bid_price
            obj['bid']['medium_per_unit_bid_price'] = the_memory.bid?.medium_per_unit_bid_price
            obj['normalized'] = [:]
            obj['normalized']['normalized_spread_bid_price'] = the_memory.normalized?.normalized_spread_bid_price
            obj['normalized']['normalized_medium_per_unit_bid_price'] = the_memory.normalized?.normalized_medium_per_unit_bid_price
            obj['normalized']['normalized_volume_ask_price'] = the_memory.normalized?.normalized_volume_ask_price
            obj['normalized']['normalized_maximum_ask_price'] = the_memory.normalized?.normalized_maximum_ask_price
            obj['normalized']['normalized_bid_medium_median_delta'] = the_memory.normalized?.normalized_bid_medium_median_delta
            obj['normalized']['normalized_minimum_bid_price'] = the_memory.normalized?.normalized_minimum_bid_price
            obj['normalized']['normalized_volume_ask_quantity'] = the_memory.normalized?.normalized_volume_ask_quantity
            obj['normalized']['normalized_medium_bid_price'] = the_memory.normalized?.normalized_medium_bid_price
            obj['normalized']['normalized_spread'] = the_memory.normalized?.normalized_spread
            obj['normalized']['normalized_median_bid_price'] = the_memory.normalized?.normalized_median_bid_price
            obj['normalized']['normalized_spread_ask_price'] = the_memory.normalized?.normalized_spread_ask_price
            obj['normalized']['normalized_maximum_bid_price'] = the_memory.normalized?.normalized_maximum_bid_price
            obj['normalized']['normalized_volume_bid_quantity'] = the_memory.normalized?.normalized_volume_bid_quantity
            obj['normalized']['normalized_ask_medium_median_delta'] = the_memory.normalized?.normalized_ask_medium_median_delta
            obj['normalized']['normalized_medium_ask_price'] = the_memory.normalized?.normalized_medium_ask_price
            obj['normalized']['normalized_medium_per_unit_ask_price'] = the_memory.normalized?.normalized_medium_per_unit_ask_price
            obj['normalized']['normalized_minimum_ask_price'] = the_memory.normalized?.normalized_minimum_ask_price
            obj['normalized']['normalized_median_ask_price'] = the_memory.normalized?.normalized_median_ask_price
            obj['normalized']['normalized_total_bid_value'] = the_memory.normalized?.normalized_total_bid_value
            obj['normalized']['normalized_total_ask_value'] = the_memory.normalized?.normalized_total_ask_value
            obj['normalized']['normalized_volume_bid_price'] = the_memory.normalized?.normalized_volume_bid_price
            obj['exchange'] = [:]
            obj['exchange']['details'] = [:]
            obj['exchange']['details']['tradecurrency'] = the_memory.exchange?.details?.tradecurrency
            obj['exchange']['details']['pricecurrency'] = the_memory.exchange?.details?.pricecurrency
            obj['exchange']['platform'] = the_memory.exchange?.platform
            obj['exchange']['exchange'] = the_memory.exchange?.exchange
            obj['metadata'] = [:]
            obj['metadata']['datetime'] = the_memory.metadata?.datetime
            obj['metadata']['hostname'] = the_memory.metadata?.hostname
            obj['graph'] = [:]
            obj['graph']['price'] = the_memory.graph?.price
            obj['graph']['quantity'] = the_memory.graph?.quantity
            this.memory.insert(obj)
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

}
