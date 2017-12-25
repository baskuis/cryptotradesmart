package com.ukora.tradestudent.services

import com.ukora.tradestudent.entities.*
import org.apache.commons.lang3.builder.EqualsBuilder
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.internal.matchers.apachecommons.ReflectionEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.ApplicationContext
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner)
@DataMongoTest
class BytesFetcherServiceSpec {

    BytesFetcherService bytesFetcherService = new BytesFetcherService()

    @Autowired
    MongoTemplate mongoTemplate

    @Autowired
    ApplicationContext applicationContext

    @Test
    void "test insertMemory/getMemory"() {


        bytesFetcherService.mongoTemplate = mongoTemplate
        bytesFetcherService.applicationContext = applicationContext
        bytesFetcherService.init()

        Memory memory = new Memory(
                metadata: new Metadata(
                        hostname: "foobar",
                        datetime: new Date()
                ),
                bid: new Bid(
                        volume_bid_quantity: 0.1d,
                        bid_medium_median_delta: 0.1d,
                        minimum_bid_price: 0.1d,
                        median_bid_price: 0.1d,
                        maximum_bid_price: 0.1d,
                        total_bid_value: 0.1d,
                        medium_bid_price: 0.1d,
                        medium_per_unit_bid_price: 0.1d
                ),
                ask: new Ask(
                        ask_medium_median_delta: 0.1d,
                        volume_ask_quantity: 0.1d,
                        minimum_ask_price: 0.1d,
                        medium_ask_price: 0.1d,
                        total_ask_value: 0.1d,
                        maximum_ask_price: 0.1d,
                        medium_per_unit_ask_price: 0.1d,
                        median_ask_price: 0.1d,
                ),
                normalized: new Normalized(
                        normalized_spread_bid_price: 0.1d,
                        normalized_medium_per_unit_bid_price: 0.1d,
                        normalized_volume_ask_price: 0.1d,
                        normalized_maximum_ask_price: 0.1d,
                        normalized_bid_medium_median_delta: 0.1d,
                        normalized_minimum_bid_price: 0.1d,
                        normalized_volume_ask_quantity: 0.1d,
                        normalized_medium_bid_price: 0.1d,
                        normalized_spread: 0.1d,
                        normalized_median_bid_price: 0.1d,
                        normalized_spread_ask_price: 0.1d,
                        normalized_maximum_bid_price: 0.1d,
                        normalized_volume_bid_quantity: 0.1d,
                        normalized_ask_medium_median_delta: 0.1d,
                        normalized_medium_ask_price: 0.1d,
                        normalized_medium_per_unit_ask_price: 0.1d,
                        normalized_minimum_ask_price: 0.1d,
                        normalized_median_ask_price: 0.1d,
                        normalized_total_bid_value: 0.1d,
                        normalized_total_ask_value: 0.1d,
                        normalized_volume_bid_price: 0.1d
                ),
                exchange: new Exchange(
                        details: new Details(
                                tradecurrency: "foo",
                                pricecurrency: "bar",
                        ),
                        platform: "foobar",
                        exchange: "foobar"
                ),
                graph: new Graph(
                        price: 0.1d,
                        quantity: 0.1d
                )
        )

        /** Insert into mongodb */
        bytesFetcherService.insertMemory(memory)

        /** Retrieve from mongodb */
        Memory retrievedMemory = bytesFetcherService.getMemory(memory.metadata.datetime)

        /** Compare all fields */
        assert retrievedMemory.graph.price == memory.graph.price

        //TODO: Add all fields to test

        /**
         *
         *         metadata: new Metadata(
         hostname: "foobar",
         datetime: new Date()
         ),
         bid: new Bid(
         volume_bid_quantity: 0.1d,
         bid_medium_median_delta: 0.1d,
         minimum_bid_price: 0.1d,
         median_bid_price: 0.1d,
         maximum_bid_price: 0.1d,
         total_bid_value: 0.1d,
         medium_bid_price: 0.1d,
         medium_per_unit_bid_price: 0.1d
         ),
         ask: new Ask(
         ask_medium_median_delta: 0.1d,
         volume_ask_quantity: 0.1d,
         minimum_ask_price: 0.1d,
         medium_ask_price: 0.1d,
         total_ask_value: 0.1d,
         maximum_ask_price: 0.1d,
         medium_per_unit_ask_price: 0.1d,
         median_ask_price: 0.1d,
         ),
         normalized: new Normalized(
         normalized_spread_bid_price: 0.1d,
         normalized_medium_per_unit_bid_price: 0.1d,
         normalized_volume_ask_price: 0.1d,
         normalized_maximum_ask_price: 0.1d,
         normalized_bid_medium_median_delta: 0.1d,
         normalized_minimum_bid_price: 0.1d,
         normalized_volume_ask_quantity: 0.1d,
         normalized_medium_bid_price: 0.1d,
         normalized_spread: 0.1d,
         normalized_median_bid_price: 0.1d,
         normalized_spread_ask_price: 0.1d,
         normalized_maximum_bid_price: 0.1d,
         normalized_volume_bid_quantity: 0.1d,
         normalized_ask_medium_median_delta: 0.1d,
         normalized_medium_ask_price: 0.1d,
         normalized_medium_per_unit_ask_price: 0.1d,
         normalized_minimum_ask_price: 0.1d,
         normalized_median_ask_price: 0.1d,
         normalized_total_bid_value: 0.1d,
         normalized_total_ask_value: 0.1d,
         normalized_volume_bid_price: 0.1d
         ),
         exchange: new Exchange(
         details: new Details(
         tradecurrency: "foo",
         pricecurrency: "bar",
         ),
         platform: "foobar",
         exchange: "foobar"
         ),
         graph: new Graph(
         price: 0.1d,
         quantity: 0.1d
         )
         *
         */


    }

}
