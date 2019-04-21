package com.ukora.tradestudent.services.learner

import com.ukora.domain.beans.tags.buysell.BuySellTagGroup
import com.ukora.domain.beans.tags.moves.DownMoveTag
import com.ukora.domain.beans.tags.moves.UpDownMovesTagGroup
import com.ukora.domain.beans.tags.moves.UpMoveTag
import com.ukora.domain.beans.tags.reversal.DownReversalTag
import com.ukora.domain.beans.tags.reversal.UpDownReversalTagGroup
import com.ukora.domain.beans.tags.reversal.UpReversalTag
import com.ukora.domain.beans.tags.trend.DownTag
import com.ukora.domain.beans.tags.trend.UpDownTagGroup
import com.ukora.domain.beans.tags.trend.UpTag
import com.ukora.domain.entities.Exchange
import com.ukora.domain.entities.Graph
import com.ukora.domain.entities.Lesson
import com.ukora.domain.entities.Memory
import com.ukora.domain.entities.Metadata
import com.ukora.tradestudent.TradestudentApplication
import com.ukora.tradestudent.services.BytesFetcherService
import spock.lang.Specification

import java.time.LocalDateTime
import java.time.ZoneId

class TraverseLessonsServiceSpec extends Specification {

    TraverseLessonsService traverseLessonsService = new TraverseLessonsService()

    UpDownTagGroup upDownTagGroup = Mock(UpDownTagGroup)
    BuySellTagGroup buySellTagGroup = Mock(BuySellTagGroup)
    UpDownReversalTagGroup upDownReversalTagGroup = Mock(UpDownReversalTagGroup)
    UpDownMovesTagGroup upDownMovesTagGroup = Mock(UpDownMovesTagGroup)
    BytesFetcherService bytesFetcherService = Mock(BytesFetcherService)

    def setup() {
        traverseLessonsService.upDownTagGroup = upDownTagGroup
        traverseLessonsService.buySellTagGroup = buySellTagGroup
        traverseLessonsService.upDownReversalTagGroup = upDownReversalTagGroup
        traverseLessonsService.upDownMovesTagGroup = upDownMovesTagGroup
        traverseLessonsService.bytesFetcherService = bytesFetcherService
        TradestudentApplication.DEBUG_LOGGING_ENABLED = true
    }

    def "test runTradeSimulation"() {

        setup:
        List<Map<String, Object>> transformedReferences = []
        (1..40).each {
            transformedReferences << [
                    date : LocalDateTime.now().plusMinutes(it),
                    price: 1000 + it
            ]
        }
        (1..80).each {
            transformedReferences << [
                    date : LocalDateTime.now().plusMinutes(40 + it),
                    price: 1040 - it
            ]
        }
        (1..200).each {
            transformedReferences << [
                    date : LocalDateTime.now().plusMinutes(120 + it),
                    price: 960 + it
            ]
        }
        (1..200).each {
            transformedReferences << [
                    date : LocalDateTime.now().plusMinutes(320 + it),
                    price: 1160 - it
            ]
        }
        (1..40).each {
            transformedReferences << [
                    date : LocalDateTime.now().plusMinutes(520 + it),
                    price: 960 + it
            ]
        }
        LearnSimulation learnSimulation = new LearnSimulation(
                interval: 20,
                tradeCount: 300,
                balanceA: 10D,
                balanceB: 0D
        )

        when:
        def r = traverseLessonsService.runTradeSimulation(transformedReferences, learnSimulation)

        then:
        int highestMultiple = 1
        transformedReferences.last().price == 1000
        r.each {
            highestMultiple = (it as Map).multiple > highestMultiple ? (it as Map).multiple : highestMultiple
        }
        println 'highest multiple:' + highestMultiple
        highestMultiple > 1
        noExceptionThrown()
        0 * _

    }

    def "test learnFromTrend"() {

        setup:
        Date startFrom = Date.from(LocalDateTime.now().minusDays(2).atZone(ZoneId.systemDefault()).toInstant())
        TradestudentApplication.DEBUG_LOGGING_ENABLED = true
        List<Memory> memories = []
        (1..40).each {
            memories << new Memory(
                    metadata: new Metadata(
                            datetime: null
                    ),
                    graph: new Graph(
                            price: 1000 + it
                    )
            )
        }
        (1..80).each {
            memories << new Memory(
                    metadata: new Metadata(
                            datetime: null
                    ),
                    graph: new Graph(
                            price: 1040 - it
                    )
            )
        }
        (1..200).each {
            memories << new Memory(
                    metadata: new Metadata(
                            datetime: null
                    ),
                    graph: new Graph(
                            price: 960 + it
                    )
            )
        }
        (1..200).each {
            memories << new Memory(
                    metadata: new Metadata(
                            datetime: null
                    ),
                    graph: new Graph(
                            price: 1160 - it
                    )
            )
        }
        (1..40).each {
            memories << new Memory(
                    metadata: new Metadata(
                            datetime: null
                    ),
                    graph: new Graph(
                            price: 960 + it
                    )
            )
        }
        (1..300).each {
            memories << new Memory(
                    metadata: new Metadata(
                            datetime: null
                    ),
                    graph: new Graph(
                            price: 1000 + it
                    )
            )
        }
        (1..300).each {
            memories << new Memory(
                    metadata: new Metadata(
                            datetime: null
                    ),
                    graph: new Graph(
                            price: 1300 - it
                    )
            )
        }
        (1..200).each {
            memories << new Memory(
                    metadata: new Metadata(
                            datetime: null
                    ),
                    graph: new Graph(
                            price: 1000 + it
                    )
            )
        }
        (1..100).each {
            memories << new Memory(
                    metadata: new Metadata(
                            datetime: null
                    ),
                    graph: new Graph(
                            price: 1200 - it
                    )
            )
        }

        when:
        int idx = -1
        traverseLessonsService.learnFromTrend(startFrom)

        then:
        _ * bytesFetcherService.getMemory( _ as Date) >> { Date d ->
            idx++
            Memory m
            try {
                m = (memories.get(idx) as Memory)
                m.metadata.datetime = d
            } catch (e) { /** ignore */ }
            return m?:null
        }
        _ * upDownTagGroup.getDownTag() >> new DownTag()
        _ * upDownTagGroup.getUpTag() >> new UpTag()
        _ * upDownReversalTagGroup.getDownReversalTag() >> new DownReversalTag()
        _ * upDownReversalTagGroup.getUpReversalTag() >> new UpReversalTag()
        _ * bytesFetcherService.saveLesson(_ as Lesson)
        noExceptionThrown()
        0 * _

    }

    def "test learnFromMarketMoves"() {

        setup:
        Date startFrom = Date.from(LocalDateTime.now().minusMinutes(90).atZone(ZoneId.systemDefault()).toInstant())
        TradestudentApplication.DEBUG_LOGGING_ENABLED = true
        List<Memory> memories = []
        (1..30).each {
            memories << new Memory(
                    metadata: new Metadata(
                            datetime: null
                    ),
                    graph: new Graph(
                            price: 1000 + (it * 10)
                    )
            )
        }
        (1..30).each {
            memories << new Memory(
                    metadata: new Metadata(
                            datetime: null
                    ),
                    graph: new Graph(
                            price: 1300
                    )
            )
        }
        (1..30).each {
            memories << new Memory(
                    metadata: new Metadata(
                            datetime: null
                    ),
                    graph: new Graph(
                            price: 1300 - (it * 10)
                    )
            )
        }

        when:
        int idx = 0
        traverseLessonsService.learnFromMarketMoves(startFrom)

        then:
        _ * bytesFetcherService.getMemory(_ as Date) >> { Date d ->
            Memory m = null
            try {
                m = (memories.get(idx++) as Memory)
                m.metadata.datetime = d
            } catch (Exception e) { /** ignore */ }
            return m
        }
        _ * upDownMovesTagGroup.getUpMoveTag() >> new UpMoveTag()
        _ * upDownMovesTagGroup.getDownMoveTag() >> new DownMoveTag()
        _ * bytesFetcherService.saveLesson(_ as Lesson)
        noExceptionThrown()
        0 * _
        idx == 91

    }

}
