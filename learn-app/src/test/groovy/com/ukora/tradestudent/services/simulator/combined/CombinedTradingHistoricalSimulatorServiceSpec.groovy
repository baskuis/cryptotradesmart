package com.ukora.tradestudent.services.simulator.combined

import com.ukora.domain.beans.trade.TradeExecution
import com.ukora.tradestudent.services.simulator.CombinedSimulation
import spock.lang.Specification
import spock.lang.Unroll

class CombinedTradingHistoricalSimulatorServiceSpec extends Specification {

    CombinedTradingHistoricalSimulatorService combinedTradingHistoricalSimulatorService = new CombinedTradingHistoricalSimulatorService()

    @Unroll
    def "test simulateTrade #type, #amount produces #totalBalance"() {

        setup:
        CombinedSimulation simulation = new CombinedSimulation(
                transactionCost: 0.002,
                balanceA: 10,
                balanceB: 5000
        )
        TradeExecution tradeExecution = new TradeExecution(
                tradeType: type,
                amount: amount,
                price: price,
                date: new Date()
        )

        when:
        combinedTradingHistoricalSimulatorService.simulateTrade(simulation, tradeExecution)

        then:
        simulation.totalBalance == totalBalance
        simulation.balanceA == balanceA
        simulation.balanceB == balanceB

        where:
        amount | type                          | price | totalBalance       | balanceA          | balanceB
        0.1d   | TradeExecution.TradeType.SELL | 2200  | 12.272527272727274 | 9.9               | 5219.56
        15d    | TradeExecution.TradeType.SELL | 2200  | 12.252727272727272 | 0.0               | 26956.0
        0.1d   | TradeExecution.TradeType.BUY  | 2200  | 12.272527272727274 | 10.0998           | 4780.0
        15d    | TradeExecution.TradeType.BUY  | 2200  | 12.26818181818182  | 12.26818181818182 | 0.0

    }

}
