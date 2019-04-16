package com.ukora.collect

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.ukora.domain.entities.Ask
import com.ukora.domain.entities.Bid
import com.ukora.domain.entities.Book
import com.ukora.domain.entities.Details
import com.ukora.domain.entities.Exchange
import com.ukora.domain.entities.Graph
import com.ukora.domain.entities.Memory
import com.ukora.domain.entities.Metadata
import com.ukora.domain.entities.Normalized
import com.ukora.domain.repositories.MemoryRepository
import org.bson.Document
import org.knowm.xchange.currency.CurrencyPair
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

import javax.annotation.PostConstruct

@EnableScheduling
@EnableMongoRepositories(basePackages = ['com.ukora.domain.repositories', 'com.ukora.collect'])
@SpringBootApplication(exclude = [
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
])
class CollectApplication {

    static void main(String[] args) {
        SpringApplication.run CollectApplication, args
    }

    @Autowired
    MongoTemplate mongoTemplate

    @Autowired
    OrderBookRepository orderBookRepository

    @Autowired
    MemoryRepository memoryRepository

    @Autowired
    ApplicationContext applicationContext

    List<SourceIntegration> integrations

    ObjectMapper objectMapper

    @PostConstruct
    void init() {
        integrations = applicationContext.getBeansOfType(SourceIntegration).values().asList()
        objectMapper = new ObjectMapper()
    }

    @Scheduled(cron = "0 * * * * *")
    void crawl() {
        List<Thread> threads = []
        integrations.each { SourceIntegration sourceIntegration ->
            if (sourceIntegration.enabled()) {
                threads.push(Thread.start {
                    if (sourceIntegration.exchange().exchangeMetaData.getCurrencyPairs().get(CurrencyPair.BTC_USD)) {
                        Book b = sourceIntegration.capture(CurrencyPair.BTC_USD)
                        if (b) {
                            capture(b)
                        }
                    }
                    if (sourceIntegration.exchange().exchangeMetaData.getCurrencyPairs().get(CurrencyPair.BTC_USDT)) {
                        Book b = sourceIntegration.capture(CurrencyPair.BTC_USDT)
                        if (b) {
                            capture(b)
                        }
                    }
                    if (sourceIntegration.exchange().exchangeMetaData.getCurrencyPairs().get(CurrencyPair.ETH_USD)) {
                        Book b = sourceIntegration.capture(CurrencyPair.ETH_USD)
                        if (b) {
                            capture(b)
                        }
                    }
                    if (sourceIntegration.exchange().exchangeMetaData.getCurrencyPairs().get(CurrencyPair.ETH_USDT)) {
                        Book b = sourceIntegration.capture(CurrencyPair.ETH_USDT)
                        if (b) {
                            capture(b)
                        }
                    }
                })
            }
        }
        threads*.join()
    }

    void capture(Book b) {
        if (!b) return
        try {
            orderBookRepository.insert(b)
        } catch(Exception e) {
            e.printStackTrace()
        }
    }

    @Scheduled(initialDelay = 10000l, fixedRate = 10000l)
    void check() {
        integrations.each {
            if (it.enabled()) {
                if (it.lastAttempted() > 0 && it.lastAttempted() > it.lastSuccess() + 60000) {
                    if (it.health().status != SourceIntegration.Health.Status.DOWN) {
                        it.health().status = SourceIntegration.Health.Status.DOWN
                        it.health().message = 'Too long since success'
                    }
                }
                if (it.health().status == SourceIntegration.Health.Status.DOWN) {
                    println 'CRITICAL: ' + it.integrationType().name() + ' is down. Message:' + it.health().message
                }
                if (it.health().status == SourceIntegration.Health.Status.UP) {
                    println it.integrationType().name() + ' is up'
                }
            } else {
                println it.integrationType().name() + ' is disabled'
            }
        }
    }

    @Scheduled(initialDelay = 10000l, fixedRate = 10000l)
    void digest() {
        orderBookRepository.findByProcessed(false)?.each {
            try {

                Double maximum_ask_price = 0
                Double minimum_ask_price = (((it?.orderBook as Map)?.asks as List)[0] as Map).limitPrice as Double
                Double asks_total_price = 0
                Double asks_total_quantity = 0
                Double asks_total_value = 0
                Double quantity = 0

                ((it?.orderBook as Map)?.asks as List)?.each { Map ask ->
                    minimum_ask_price = (ask.limitPrice as Double) < minimum_ask_price ? (ask.limitPrice as Double) : minimum_ask_price
                    maximum_ask_price = (ask.limitPrice as Double) > maximum_ask_price ? (ask.limitPrice as Double) : maximum_ask_price
                    asks_total_price += (ask.limitPrice as Double)
                    asks_total_quantity += (ask.originalAmount as Double)
                    asks_total_value += (ask.limitPrice as Double) * (ask.originalAmount as Double)
                    quantity += (ask.originalAmount as Double)
                }

                Double median_ask_price = (((it?.orderBook as Map)?.asks as List).get(Math.ceil(((it?.orderBook as Map)?.asks as List).size() / 2) as int) as Map).limitPrice as Double
                Double medium_ask_price = asks_total_price / ((it?.orderBook as Map)?.asks as List).size()
                Double volume_ask_price = asks_total_price
                Double volume_ask_quantity = asks_total_quantity
                Double total_ask_value = asks_total_value
                Double medium_per_unit_ask_price = total_ask_value / asks_total_quantity
                Double ask_medium_median_delta = medium_ask_price - median_ask_price

                Double maximum_bid_price = 0L
                Double minimum_bid_price = (((it?.orderBook as Map)?.bids as List)[0] as Map).limitPrice as Double
                Double bids_total_price = 0L
                Double bids_total_quantity = 0L
                Double bids_total_value = 0L
                Double spread_ask_price = maximum_ask_price - minimum_ask_price

                ((it?.orderBook as Map)?.bids as List)?.each { Map bid ->
                    minimum_bid_price = (bid.limitPrice as Double) < minimum_bid_price ? (bid.limitPrice as Double) : minimum_bid_price
                    maximum_bid_price = (bid.limitPrice as Double) > maximum_bid_price ? (bid.limitPrice as Double) : maximum_bid_price
                    bids_total_price += (bid.limitPrice as Double)
                    bids_total_quantity += (bid.originalAmount as Double)
                    bids_total_value += (bid.limitPrice as Double) * (bid.originalAmount as Double)
                    quantity += (bid.originalAmount as Double)
                }

                Double median_bid_price = (((it?.orderBook as Map)?.bids as List).get(Math.ceil(((it?.orderBook as Map)?.bids as List).size() / 2) as int) as Map).limitPrice as Double
                Double medium_bid_price = bids_total_price / ((it?.orderBook as Map)?.bids as List).size()
                Double volume_bid_price = bids_total_price
                Double volume_bid_quantity = bids_total_quantity
                Double total_bid_value = bids_total_value
                Double medium_per_unit_bid_price = total_bid_value / bids_total_quantity
                Double bid_medium_median_delta = medium_bid_price - median_bid_price
                Double spread_bid_price = maximum_bid_price - minimum_bid_price

                Double price = (maximum_bid_price + minimum_ask_price) / 2D
                Double spread = minimum_ask_price - maximum_bid_price

                Double normalized_spread = spread / price

                Double normalized_maximum_ask_price = maximum_ask_price / price
                Double normalized_minimum_ask_price = minimum_ask_price / price
                Double normalized_medium_ask_price = medium_ask_price / price
                Double normalized_medium_per_unit_ask_price = medium_per_unit_ask_price / price
                Double normalized_median_ask_price = median_ask_price / price
                Double normalized_volume_ask_price = volume_ask_price / price
                Double normalized_volume_ask_quantity = volume_ask_quantity / quantity
                Double normalized_spread_ask_price = spread_ask_price / price
                Double normalized_total_ask_value = total_ask_value / price
                Double normalized_ask_medium_median_delta = ask_medium_median_delta / price

                Double normalized_maximum_bid_price = maximum_bid_price / price
                Double normalized_minimum_bid_price = minimum_bid_price / price
                Double normalized_medium_bid_price = medium_bid_price / price
                Double normalized_medium_per_unit_bid_price = medium_per_unit_bid_price / price
                Double normalized_median_bid_price = median_bid_price / price
                Double normalized_volume_bid_price = volume_bid_price / price
                Double normalized_volume_bid_quantity = volume_bid_quantity / quantity
                Double normalized_spread_bid_price = spread_bid_price / price
                Double normalized_total_bid_value = total_bid_value / price
                Double normalized_bid_medium_median_delta = bid_medium_median_delta / price

                Double normalized_total_ask_to_bid_ratio = normalized_total_ask_value / normalized_total_bid_value

                Memory memory = new Memory(
                        metadata: new Metadata(
                                hostname: InetAddress.getLocalHost().getHostName(),
                                datetime: it.timestamp as Date
                        ),
                        bid: new Bid(
                                volume_bid_quantity: volume_bid_quantity,
                                bid_medium_median_delta: bid_medium_median_delta,
                                minimum_bid_price: minimum_bid_price,
                                median_bid_price: median_bid_price,
                                maximum_bid_price: maximum_bid_price,
                                total_bid_value: total_bid_value,
                                medium_bid_price: medium_bid_price,
                                medium_per_unit_bid_price: medium_per_unit_bid_price
                        ),
                        ask: new Ask(
                                ask_medium_median_delta: ask_medium_median_delta,
                                volume_ask_quantity: volume_ask_quantity,
                                minimum_ask_price: minimum_ask_price,
                                medium_ask_price: medium_ask_price,
                                total_ask_value: total_ask_value,
                                maximum_ask_price: maximum_ask_price,
                                medium_per_unit_ask_price: medium_per_unit_ask_price,
                                median_ask_price: median_ask_price,
                        ),
                        normalized: new Normalized(
                                normalized_spread_bid_price: normalized_spread_bid_price,
                                normalized_medium_per_unit_bid_price: normalized_medium_per_unit_bid_price,
                                normalized_volume_ask_price: normalized_volume_ask_price,
                                normalized_maximum_ask_price: normalized_maximum_ask_price,
                                normalized_bid_medium_median_delta: normalized_bid_medium_median_delta,
                                normalized_minimum_bid_price: normalized_minimum_bid_price,
                                normalized_volume_ask_quantity: normalized_volume_ask_quantity,
                                normalized_medium_bid_price: normalized_medium_bid_price,
                                normalized_spread: normalized_spread,
                                normalized_median_bid_price: normalized_median_bid_price,
                                normalized_spread_ask_price: normalized_spread_ask_price,
                                normalized_maximum_bid_price: normalized_maximum_bid_price,
                                normalized_volume_bid_quantity: normalized_volume_bid_quantity,
                                normalized_ask_medium_median_delta: normalized_ask_medium_median_delta,
                                normalized_medium_ask_price: normalized_medium_ask_price,
                                normalized_medium_per_unit_ask_price: normalized_medium_per_unit_ask_price,
                                normalized_minimum_ask_price: normalized_minimum_ask_price,
                                normalized_median_ask_price: normalized_median_ask_price,
                                normalized_total_bid_value: normalized_total_bid_value,
                                normalized_total_ask_value: normalized_total_ask_value,
                                normalized_volume_bid_price: normalized_volume_bid_price,
                                normalized_total_ask_to_bid_ratio: normalized_total_ask_to_bid_ratio
                        ),
                        exchange: new Exchange(
                                details: new Details(
                                        tradecurrency: it.baseCode,
                                        pricecurrency: it.counterCode,
                                ),
                                platform: it.integrationType,
                                exchange: it.integrationType
                        ),
                        graph: new Graph(
                                price: price,
                                quantity: quantity
                        )
                )

                //mark processed
                if( it.processed ) throw new Exception('Already processed')

                //Insert memory
                Memory i = memoryRepository.insert(memory)
                println 'memory inserted: ' + i.id + ' ts:' + i.metadata.datetime

            } catch (Exception e) {
                println "Issue parsing order book"
                e.printStackTrace()
            } finally {
                it.processed = true
                orderBookRepository.save(it)
            }
        }
    }

}
