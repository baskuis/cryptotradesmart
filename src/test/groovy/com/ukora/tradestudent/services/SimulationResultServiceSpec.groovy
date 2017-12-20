package com.ukora.tradestudent.services

import com.ukora.tradestudent.entities.SimulationResult
import spock.lang.Specification

class SimulationResultServiceSpec extends Specification {

    SimulationResultService simulationResultService = new SimulationResultService()

    BytesFetcherService bytesFetcherService = Mock(BytesFetcherService)

    List<SimulationResult> mockSimulations = [
            new SimulationResult(
                    endDate: new Date(),
                    buyThreshold: 0.54,
                    sellThreshold: 0.50,
                    differential: 1.04
            ),
            new SimulationResult(
                    endDate: new Date(),
                    buyThreshold: 0.58,
                    sellThreshold: 0.62,
                    differential: 1.04
            ),
            new SimulationResult(
                    endDate: new Date(),
                    buyThreshold: 0.88,
                    sellThreshold: 0.52,
                    differential: 1.04
            ),
            new SimulationResult(
                    endDate: new Date(),
                    buyThreshold: 0.88,
                    sellThreshold: 0.90,
                    differential: 1.03
            )
    ]

    def setup() {
        simulationResultService.bytesFetcherService = bytesFetcherService
    }

    def "Test getTopPerformingSimulations"() {

        when:
        def simulations = simulationResultService.getTopPerformingSimulations()

        then:
        1 * bytesFetcherService.getSimulations() >> mockSimulations
        simulations != null
        simulations.first().differential == 1.03

    }

    def "Test getTopPerformingSimulation"() {

        when:
        def simulation = simulationResultService.getTopPerformingSimulation()

        then:
        1 * bytesFetcherService.getSimulations() >> mockSimulations
        simulation != null
        simulation.differential == 1.03

    }

    def "Test getTopPerformingSimulation accepts empty"() {

        when:
        def simulation = simulationResultService.getTopPerformingSimulation()

        then:
        1 * bytesFetcherService.getSimulations() >> null
        simulation == null
        noExceptionThrown()

    }

}
