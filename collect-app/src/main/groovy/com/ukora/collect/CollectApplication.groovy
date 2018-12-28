package com.ukora.collect

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import org.bson.Document
import org.knowm.xchange.currency.CurrencyPair
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

import javax.annotation.PostConstruct

@SpringBootApplication(exclude = [
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
])
@EnableScheduling
class CollectApplication {

    static void main(String[] args) {
        SpringApplication.run CollectApplication, args
    }

    @Autowired
    MongoTemplate mongoTemplate

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
                        SourceIntegration.Book book = sourceIntegration.capture(CurrencyPair.BTC_USD)
                        if (book) {
                            capture(book)
                        }
                    }
                    if (sourceIntegration.exchange().exchangeMetaData.getCurrencyPairs().get(CurrencyPair.BTC_USDT)) {
                        SourceIntegration.Book book = sourceIntegration.capture(CurrencyPair.BTC_USDT)
                        if (book) {
                            capture(book)
                        }
                    }
                    if (sourceIntegration.exchange().exchangeMetaData.getCurrencyPairs().get(CurrencyPair.ETH_USD)) {
                        SourceIntegration.Book book = sourceIntegration.capture(CurrencyPair.ETH_USD)
                        if (book) {
                            capture(book)
                        }
                    }
                    if (sourceIntegration.exchange().exchangeMetaData.getCurrencyPairs().get(CurrencyPair.ETH_USDT)) {
                        SourceIntegration.Book book = sourceIntegration.capture(CurrencyPair.ETH_USDT)
                        if (book) {
                            capture(book)
                        }
                    }
                })
            }
        }
        threads*.join()
    }

    void capture(SourceIntegration.Book book) {
        if (!book) return
        mongoTemplate.getCollection('books').insertOne(Document.parse(objectMapper.writeValueAsString(book)))
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
        mongoTemplate.getCollection('books').find(Filters.ne('processed', '1')).limit(10).each {

            try {

                Float maximum_ask_price = 0
                Float minimum_ask_price = (((it?.orderBook as Map)?.asks as List)[0] as Map).limitPrice as Float
                Float asks_total_price = 0
                Float asks_total_quantity = 0
                Float asks_total_value = 0
                Float quantity = 0

                ((it?.orderBook as Map)?.asks as List)?.each { Map ask ->
                    minimum_ask_price = ask.limitPrice < minimum_ask_price ? ask.limitPrice : minimum_ask_price
                    maximum_ask_price = ask.limitPrice > maximum_ask_price ? ask.limitPrice : maximum_ask_price
                    asks_total_price += ask.limitPrice
                    asks_total_quantity += ask.originalAmount
                    asks_total_value += ask.limitPrice * ask.originalAmount
                    quantity += ask.originalAmount
                }

                Float median_ask_price = (((it?.orderBook as Map)?.asks as List).get(Math.ceil(((it?.orderBook as Map)?.asks as List).size() / 2) as int) as Map).limitPrice
                Float medium_ask_price = asks_total_price / ((it?.orderBook as Map)?.asks as List).size()
                Float volume_ask_price = asks_total_price
                Float volume_ask_quantity = asks_total_quantity
                Float total_ask_value = asks_total_value
                Float medium_per_unit_ask_price = total_ask_value / asks_total_quantity
                Float ask_medium_median_delta = medium_ask_price - median_ask_price

                Float maximum_bid_price = 0L
                Float minimum_bid_price = (((it?.orderBook as Map)?.bids as List)[0] as Map).limitPrice as Float
                Float bids_total_price = 0L
                Float bids_total_quantity = 0L
                Float bids_total_value = 0L
                Float spread_ask_price = maximum_ask_price - minimum_ask_price

                ((it?.orderBook as Map)?.bids as List)?.each { Map bid ->
                    minimum_bid_price = bid.limitPrice < minimum_bid_price ? bid.limitPrice : minimum_bid_price
                    maximum_bid_price = bid.limitPrice > maximum_bid_price ? bid.limitPrice : maximum_bid_price
                    bids_total_price += bid.limitPrice
                    bids_total_quantity += bid.originalAmount
                    bids_total_value += bid.limitPrice * bid.originalAmount
                    quantity += bid.originalAmount
                }

                Float median_bid_price = (((it?.orderBook as Map)?.bids as List).get(Math.ceil(((it?.orderBook as Map)?.bids as List).size() / 2) as int) as Map).limitPrice
                Float medium_bid_price = bids_total_price / ((it?.orderBook as Map)?.bids as List).size()
                Float volume_bid_price = bids_total_price
                Float volume_bid_quantity = bids_total_quantity
                Float total_bid_value = bids_total_value
                Float medium_per_unit_bid_price = total_bid_value / bids_total_quantity
                Float bid_medium_median_delta = medium_bid_price - median_bid_price
                Float spread_bid_price = maximum_bid_price - minimum_bid_price

                Float price = (maximum_bid_price + minimum_ask_price) / 2
                Float spread = minimum_ask_price - maximum_bid_price

                Float normalized_spread = spread / price

                Float normalized_maximum_ask_price = maximum_ask_price / price
                Float normalized_minimum_ask_price = minimum_ask_price / price
                Float normalized_medium_ask_price = medium_ask_price / price
                Float normalized_medium_per_unit_ask_price = medium_per_unit_ask_price / price
                Float normalized_median_ask_price = median_ask_price / price
                Float normalized_volume_ask_price = volume_ask_price / price
                Float normalized_volume_ask_quantity = volume_ask_quantity / quantity
                Float normalized_spread_ask_price = spread_ask_price / price
                Float normalized_total_ask_value = total_ask_value / price
                Float normalized_ask_medium_median_delta = ask_medium_median_delta / price

                Float normalized_maximum_bid_price = maximum_bid_price / price
                Float normalized_minimum_bid_price = minimum_bid_price / price
                Float normalized_medium_bid_price = medium_bid_price / price
                Float normalized_medium_per_unit_bid_price = medium_per_unit_bid_price / price
                Float normalized_median_bid_price = median_bid_price / price
                Float normalized_volume_bid_price = volume_bid_price / price
                Float normalized_volume_bid_quantity = volume_bid_quantity / quantity
                Float normalized_spread_bid_price = spread_bid_price / price
                Float normalized_total_bid_value = total_bid_value / price
                Float normalized_bid_medium_median_delta = bid_medium_median_delta / price

                Float normalized_total_ask_to_bid_ratio = normalized_total_ask_value / normalized_total_bid_value

                Map memory = [
                        "exchange"  : [
                                "platform"    : it.integrationType,
                                "currencyPair": (((it?.orderBook as Map)?.asks as List)?.get(0) as Map)?.currencyPair
                        ],
                        "bid"       : [
                                "volume_bid_quantity"      : volume_bid_quantity,
                                "bid_medium_median_delta"  : bid_medium_median_delta,
                                "minimum_bid_price"        : minimum_bid_price,
                                "maximum_bid_price"        : maximum_bid_price,
                                "total_bid_value"          : total_bid_value,
                                "medium_bid_price"         : medium_bid_price,
                                "medium_per_unit_bid_price": medium_per_unit_bid_price
                        ],
                        "normalized": [
                                "normalized_spread_bid_price"         : normalized_spread_bid_price,
                                "normalized_medium_per_unit_bid_price": normalized_medium_per_unit_bid_price,
                                "normalized_volume_ask_price"         : normalized_volume_ask_price,
                                "normalized_maximum_ask_price"        : normalized_maximum_ask_price,
                                "normalized_bid_medium_median_delta"  : normalized_bid_medium_median_delta,
                                "normalized_minimum_bid_price"        : normalized_minimum_bid_price,
                                "normalized_volume_ask_quantity"      : normalized_volume_ask_quantity,
                                "normalized_medium_bid_price"         : normalized_medium_bid_price,
                                "normalized_spread"                   : normalized_spread,
                                "normalized_median_bid_price"         : normalized_median_bid_price,
                                "normalized_spread_ask_price"         : normalized_spread_ask_price,
                                "normalized_maximum_bid_price"        : normalized_maximum_bid_price,
                                "normalized_volume_bid_quantity"      : normalized_volume_bid_quantity,
                                "normalized_ask_medium_median_delta"  : normalized_ask_medium_median_delta,
                                "normalized_medium_ask_price"         : normalized_medium_ask_price,
                                "normalized_medium_per_unit_ask_price": normalized_medium_per_unit_ask_price,
                                "normalized_minimum_ask_price"        : normalized_minimum_ask_price,
                                "normalized_median_ask_price"         : normalized_median_ask_price,
                                "normalized_total_bid_value"          : normalized_total_bid_value,
                                "normalized_total_ask_value"          : normalized_total_ask_value,
                                "normalized_volume_bid_price"         : normalized_volume_bid_price,
                                "normalized_total_ask_to_bid_ratio"   : normalized_total_ask_to_bid_ratio
                        ],
                        "ask"       : [
                                "ask_medium_median_delta"  : ask_medium_median_delta,
                                "volume_ask_quantity"      : volume_ask_quantity,
                                "minimum_ask_price"        : minimum_ask_price,
                                "medium_ask_price"         : medium_ask_price,
                                "total_ask_value"          : total_ask_value,
                                "median_ask_price"         : median_ask_price,
                                "maximum_ask_price"        : maximum_ask_price,
                                "medium_per_unit_ask_price": medium_per_unit_ask_price
                        ],
                        "metadata"  : [
                                "hostname": InetAddress.getLocalHost().getHostName(),
                                "datetime": new Date((it?.timestamp ?: 1) as Long)
                        ]
                ]

                //Update book record
                mongoTemplate.getCollection('books').updateOne(
                        Filters.eq('_id', it?._id),
                        Updates.set('processed', '1')
                )

                //Insert into memory
                mongoTemplate.getCollection('memory').insertOne(
                        Document.parse(
                                objectMapper.writeValueAsString(memory)
                        )
                )

            } catch (Exception e) {
                println "Issue parsing order book"
                e.printStackTrace()
            }

        }
    }

}
