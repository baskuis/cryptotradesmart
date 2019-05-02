package com.ukora.tradestudent.services

import com.ukora.domain.beans.tags.buysell.BuyTag
import com.ukora.domain.entities.*
import com.ukora.domain.repositories.CorrelationAssociationRepository
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner)
@SpringBootTest
class BytesFetcherServiceSpec {

    @Autowired
    BytesFetcherService bytesFetcherService

    @Autowired
    CorrelationAssociationRepository correlationAssociationRepository

    @Test
    void testInsertThenReadProperty() {

        Property p = new Property(
                name: "foo",
                value: "bar"
        )

        bytesFetcherService.saveProp(p)

        Property r = bytesFetcherService.getProp('foo')

        assert p.name == r.name
        assert p.value == r.value
        assert r.id != null

        bytesFetcherService.saveProp('foo', 'fizz')
        Property f = bytesFetcherService.getProp('foo')

        assert f.name == 'foo'
        assert f.value == 'fizz'
        assert f.id == r.id

        bytesFetcherService.saveProp(p)
        r = bytesFetcherService.getProp('foo')

        assert r.id == f.id
        assert r.value == 'bar'
        assert r.name == 'foo'

    }

    @Test
    void testInsertThenReadLesson() {

        Date lessonDate = new Date()

        Lesson lesson = new Lesson(
                date: lessonDate,
                tag: new BuyTag(),
                processed: false
        )

        bytesFetcherService.saveLesson(lesson)

        Lesson nextLesson = bytesFetcherService.getNextLesson()

        println nextLesson

    }

    @Test
    void testInsertThenGetLesson() {

        Date d = new Date()

        Lesson lesson = new Lesson(
                tag: new BuyTag(),
                price: 100l,
                date: d,
                textProcessed: true,
                processed: false
        )

        bytesFetcherService.saveLesson(lesson)

        Lesson nextLesson = bytesFetcherService.getNextLesson()

        assert nextLesson != null

    }

    @Test
    void testNextTextLesson() {

        Date d = new Date()

        Lesson lesson = new Lesson(
                tag: new BuyTag(),
                price: 100l,
                date: d,
                textProcessed: false,
                processed: true
        )

        bytesFetcherService.saveLesson(lesson)

        Lesson nextTextLesson = bytesFetcherService.getNextTextLesson()

        assert nextTextLesson != null

    }

    @Test
    void testInsertThenReadMemory() {

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
                                tradecurrency: "BTC",
                                pricecurrency: "USD",
                        ),
                        platform: "COINBASEPRO",
                        exchange: "COINBASEPRO"
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
        assert retrievedMemory?.metadata?.hostname == memory?.metadata?.hostname

        /** Date String->Date conversion loses milliseconds */
        assert Math.floor(retrievedMemory.metadata.datetime.time / 1000) == Math.floor(memory.metadata.datetime.time / 1000)

        assert retrievedMemory.bid.volume_bid_quantity == memory.bid.volume_bid_quantity
        assert retrievedMemory.bid.bid_medium_median_delta == memory.bid.bid_medium_median_delta
        assert retrievedMemory.bid.minimum_bid_price == memory.bid.minimum_bid_price
        assert retrievedMemory.bid.median_bid_price == memory.bid.median_bid_price
        assert retrievedMemory.bid.maximum_bid_price == memory.bid.maximum_bid_price
        assert retrievedMemory.bid.total_bid_value == memory.bid.total_bid_value
        assert retrievedMemory.bid.medium_bid_price == memory.bid.medium_bid_price
        assert retrievedMemory.bid.medium_per_unit_bid_price == memory.bid.medium_per_unit_bid_price

        assert retrievedMemory.ask.ask_medium_median_delta == memory.ask.ask_medium_median_delta
        assert retrievedMemory.ask.volume_ask_quantity == memory.ask.volume_ask_quantity
        assert retrievedMemory.ask.minimum_ask_price == memory.ask.minimum_ask_price
        assert retrievedMemory.ask.medium_ask_price == memory.ask.medium_ask_price
        assert retrievedMemory.ask.total_ask_value == memory.ask.total_ask_value
        assert retrievedMemory.ask.maximum_ask_price == memory.ask.maximum_ask_price
        assert retrievedMemory.ask.medium_per_unit_ask_price == memory.ask.medium_per_unit_ask_price
        assert retrievedMemory.ask.median_ask_price == memory.ask.median_ask_price

        assert retrievedMemory.normalized.normalized_spread_bid_price == memory.normalized.normalized_spread_bid_price
        assert retrievedMemory.normalized.normalized_medium_per_unit_bid_price == memory.normalized.normalized_medium_per_unit_bid_price
        assert retrievedMemory.normalized.normalized_volume_ask_price == memory.normalized.normalized_volume_ask_price
        assert retrievedMemory.normalized.normalized_maximum_ask_price == memory.normalized.normalized_maximum_ask_price
        assert retrievedMemory.normalized.normalized_bid_medium_median_delta == memory.normalized.normalized_bid_medium_median_delta
        assert retrievedMemory.normalized.normalized_minimum_bid_price == memory.normalized.normalized_minimum_bid_price
        assert retrievedMemory.normalized.normalized_volume_ask_quantity == memory.normalized.normalized_volume_ask_quantity
        assert retrievedMemory.normalized.normalized_medium_bid_price == memory.normalized.normalized_medium_bid_price
        assert retrievedMemory.normalized.normalized_spread == memory.normalized.normalized_spread
        assert retrievedMemory.normalized.normalized_median_bid_price == memory.normalized.normalized_median_bid_price
        assert retrievedMemory.normalized.normalized_spread_ask_price == memory.normalized.normalized_spread_ask_price
        assert retrievedMemory.normalized.normalized_maximum_bid_price == memory.normalized.normalized_maximum_bid_price
        assert retrievedMemory.normalized.normalized_volume_bid_quantity == memory.normalized.normalized_volume_bid_quantity
        assert retrievedMemory.normalized.normalized_ask_medium_median_delta == memory.normalized.normalized_ask_medium_median_delta
        assert retrievedMemory.normalized.normalized_medium_ask_price == memory.normalized.normalized_medium_ask_price
        assert retrievedMemory.normalized.normalized_medium_per_unit_ask_price == memory.normalized.normalized_medium_per_unit_ask_price
        assert retrievedMemory.normalized.normalized_minimum_ask_price == memory.normalized.normalized_minimum_ask_price
        assert retrievedMemory.normalized.normalized_median_ask_price == memory.normalized.normalized_median_ask_price
        assert retrievedMemory.normalized.normalized_total_bid_value == memory.normalized.normalized_total_bid_value
        assert retrievedMemory.normalized.normalized_total_ask_value == memory.normalized.normalized_total_ask_value
        assert retrievedMemory.normalized.normalized_volume_bid_price == memory.normalized.normalized_volume_bid_price

        assert retrievedMemory.exchange.details.tradecurrency == memory.exchange.details.tradecurrency
        assert retrievedMemory.exchange.details.pricecurrency == memory.exchange.details.pricecurrency
        assert retrievedMemory.exchange.platform == memory.exchange.platform
        assert retrievedMemory.exchange.exchange == memory.exchange.exchange

        assert retrievedMemory.graph.quantity == memory.graph.quantity
        assert retrievedMemory.graph.price == memory.graph.price

    }

    @Test
    void testGetLatestMemory() {

        when:
        Memory memory = bytesFetcherService.getLatestMemory()

        then:
        memory != null
        memory.metadata.datetime != null

    }

    @Test
    void testCaptureCorrelationAssociation() {

        setup:
        Date date = new Date()
        CorrelationAssociation correlationAssociation = new CorrelationAssociation(
                memory: new Memory(
                        metadata: new Metadata(
                                datetime: date
                        )
                )
        )

        when:
        bytesFetcherService.captureCorrelationAssociation(correlationAssociation)

        and:
        correlationAssociation.tagProbabilities = [combiner: [tag: 2d]]
        bytesFetcherService.captureCorrelationAssociation(correlationAssociation)

        and:
        def r = correlationAssociationRepository.findByDate(date)

        then:
        r != null
        r.size() == 1
        r.first().date == date
        r.first().tagProbabilities.combiner.tag == 2d

    }

}
