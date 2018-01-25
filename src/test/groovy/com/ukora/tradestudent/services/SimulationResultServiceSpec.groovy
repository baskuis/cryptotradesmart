package com.ukora.tradestudent.services

import com.ukora.tradestudent.entities.SimulationResult
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant
import java.time.temporal.ChronoUnit

class SimulationResultServiceSpec extends Specification {

    SimulationResultService simulationResultService = new SimulationResultService()

    BytesFetcherService bytesFetcherService = Mock(BytesFetcherService)

    static Instant now = Instant.now()

    List<SimulationResult> mockSimulations = [
            new SimulationResult(
                    startDate: Date.from(now.minus(2, ChronoUnit.DAYS)),
                    endDate: Date.from(now),
                    buyThreshold: 0.59,
                    sellThreshold: 0.58,
                    differential: 1.05,
                    tradeCount: 20
            ),
            new SimulationResult(
                    startDate: Date.from(now.minus(1, ChronoUnit.DAYS)),
                    endDate: Date.from(now),
                    buyThreshold: 0.54,
                    sellThreshold: 0.50,
                    differential: 1.04,
                    tradeCount: 20
            ),
            new SimulationResult(
                    startDate: Date.from(now.minus(1, ChronoUnit.DAYS)),
                    endDate: Date.from(now),
                    buyThreshold: 0.58,
                    sellThreshold: 0.62,
                    differential: 1.04,
                    tradeCount: 20
            ),
            new SimulationResult(
                    startDate: Date.from(now.minus(1, ChronoUnit.DAYS)),
                    endDate: Date.from(now),
                    buyThreshold: 0.88,
                    sellThreshold: 0.52,
                    differential: 1.04,
                    tradeCount: 20
            ),
            new SimulationResult(
                    startDate: Date.from(now.minus(1, ChronoUnit.DAYS)),
                    endDate: Date.from(now),
                    buyThreshold: 0.88,
                    sellThreshold: 0.90,
                    differential: 1.03,
                    tradeCount: 20
            ),
            new SimulationResult(
                    startDate: Date.from(now.minus(1, ChronoUnit.DAYS)),
                    endDate: Date.from(now),
                    buyThreshold: 0.88,
                    sellThreshold: 0.90,
                    differential: 1.025,
                    tradeCount: 40
            )
    ]

    def setup() {
        simulationResultService.bytesFetcherService = bytesFetcherService
    }

    def "No exception is thrown when no simulations are found and an empty list is returned"() {

        when:
        simulationResultService.getTopPerformingSimulations()

        then:
        1 * bytesFetcherService.getSimulations() >> []
        noExceptionThrown()

    }

    def "No exception is thrown when no simulations are found and a null list is returned"() {

        when:
        simulationResultService.getTopPerformingSimulations()

        then:
        1 * bytesFetcherService.getSimulations() >> null
        noExceptionThrown()

    }

    def "Test getTopPerformingSimulations"() {

        when:
        def simulations = simulationResultService.getTopPerformingSimulations()

        then:
        1 * bytesFetcherService.getSimulations() >> mockSimulations
        simulations != null
        simulations.first().differential == 1.025

    }

    def "Test getTopPerformingSimulation"() {

        when:
        def simulation = simulationResultService.getTopPerformingSimulation()

        then:
        1 * bytesFetcherService.getSimulations() >> mockSimulations
        simulation != null
        simulation.differential == 1.025

    }

    def "Test getTopPerformingSimulation accepts empty"() {

        when:
        def simulation = simulationResultService.getTopPerformingSimulation()

        then:
        1 * bytesFetcherService.getSimulations() >> null
        simulation == null
        noExceptionThrown()

    }

    @Unroll
    def "Test timeDeltaIn #from to #to is #expected #unit"() {

        when:
        def delta = simulationResultService.timeDeltaIn(from, to, unit)

        then:
        delta == expected

        where:
        now           | from                                 | to             | unit               | expected
        Instant.now() | Date.from(now.minusMillis(86400000)) | Date.from(now) | ChronoUnit.DAYS    | 1
        Instant.now() | Date.from(now.minusMillis(86400000)) | Date.from(now) | ChronoUnit.HOURS   | 24
        Instant.now() | Date.from(now.minusMillis(86400000)) | Date.from(now) | ChronoUnit.SECONDS | 86400

    }

}
