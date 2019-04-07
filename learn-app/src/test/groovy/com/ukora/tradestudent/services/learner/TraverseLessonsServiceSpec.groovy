package com.ukora.tradestudent.services.learner

import com.ukora.tradestudent.TradestudentApplication
import spock.lang.Specification

import java.time.LocalDateTime

class TraverseLessonsServiceSpec extends Specification {

    TraverseLessonsService traverseLessonsService = new TraverseLessonsService()

    def "test runTradeSimulation"() {

        setup:
        TradestudentApplication.DEBUG_LOGGING_ENABLED = true
        List<Map<String, Object>> transformedReferences = []
        (1..40).each {
            transformedReferences << [
                    date : LocalDateTime.now().plusMinutes(it),
                    price: 1000 + it
            ]
        }
        (1..80).each {
            transformedReferences << [
                    date : LocalDateTime.now().plusMinutes(it),
                    price: 1040 - it
            ]
        }
        (1..200).each {
            transformedReferences << [
                    date : LocalDateTime.now().plusMinutes(it),
                    price: 960 + it
            ]
        }
        (1..200).each {
            transformedReferences << [
                    date : LocalDateTime.now().plusMinutes(it),
                    price: 1160 - it
            ]
        }
        (1..40).each {
            transformedReferences << [
                    date : LocalDateTime.now().plusMinutes(it),
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
        transformedReferences.last().price == 1000
        r
        noExceptionThrown()
        0 * _

    }

}
