package com.ukora.collect

import com.fasterxml.jackson.databind.ObjectMapper
import org.bson.Document
import org.knowm.xchange.currency.CurrencyPair
import org.knowm.xchange.dto.marketdata.OrderBook
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
            if(sourceIntegration.enabled()) {
                threads.push(Thread.start {
                    if(sourceIntegration.exchange().exchangeMetaData.getCurrencyPairs().get(CurrencyPair.BTC_USD)) {
                        SourceIntegration.Book book = sourceIntegration.capture(CurrencyPair.BTC_USD)
                        if (book) {
                            capture(book)
                        }
                    }
                    if(sourceIntegration.exchange().exchangeMetaData.getCurrencyPairs().get(CurrencyPair.BTC_USDT)) {
                        SourceIntegration.Book book = sourceIntegration.capture(CurrencyPair.BTC_USDT)
                        if (book) {
                            capture(book)
                        }
                    }
                    if(sourceIntegration.exchange().exchangeMetaData.getCurrencyPairs().get(CurrencyPair.ETH_USD)) {
                        SourceIntegration.Book book = sourceIntegration.capture(CurrencyPair.ETH_USD)
                        if (book) {
                            capture(book)
                        }
                    }
                    if(sourceIntegration.exchange().exchangeMetaData.getCurrencyPairs().get(CurrencyPair.ETH_USDT)) {
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
        if(!book) return
        mongoTemplate.getCollection('books').insertOne(Document.parse(objectMapper.writeValueAsString(book)))
    }

    @Scheduled(initialDelay = 10000l, fixedRate = 10000l)
    void check() {
        integrations.each {
            if (it.enabled()) {
                if (it.lastAttempted() > 0 && it.lastAttempted() > it.lastSuccess() + 60000) {
                    if(it.health().status != SourceIntegration.Health.Status.DOWN) {
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

}
