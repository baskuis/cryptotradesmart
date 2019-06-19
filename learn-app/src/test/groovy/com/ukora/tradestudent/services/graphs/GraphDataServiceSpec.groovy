package com.ukora.tradestudent.services.graphs

import spock.lang.Specification

class GraphDataServiceSpec extends Specification {

    GraphDataService graphDataService = new GraphDataService()

    def "test getRange"() {

        setup:
        GraphDataService.DataPoints = []
        (1..1000).each {
            GraphDataService.DataPoints.push(new GraphDataService.DataPoint(
                    date: new Date(),
                    price: 1000d,
                    numericalProbability: 0.1d,
                    textNewsProbability: 0.2d,
                    textTwitterProbability: 0.3d,
                    combinedProbability: 0.4d
            ))
        }

        when:
        def r = graphDataService.getRange(GraphDataService.Range.DAILY)

        then:
        noExceptionThrown()
        r

    }

}
