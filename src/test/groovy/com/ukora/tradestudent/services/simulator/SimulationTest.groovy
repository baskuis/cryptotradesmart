package com.ukora.tradestudent.services.simulator

import spock.lang.Specification


class SimulationTest extends Specification {

    def "test Simulation toString"() {

        given:
        Simulation simulation = new Simulation()
        simulation.key = "somekey"
        simulation.buyThreshold = 0.5
        simulation.sellThreshold = 0.5
        simulation.startingBalance = 10
        simulation.tradeIncrement = 1
        simulation.transactionCost = 0.002
        simulation.finalPrice = 10

        when:
        simulation.toString()

        then:
        noExceptionThrown()

    }

}
