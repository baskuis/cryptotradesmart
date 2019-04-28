package com.ukora.tradestudent.services.simulator

import com.ukora.domain.beans.trade.TradeExecution
import spock.lang.Specification
import spock.lang.Unroll

class BuySellTradingHistoricalSimulatorServiceTest extends Specification {

    com.ukora.tradestudent.services.simulator.origin.BuySellTradingHistoricalSimulatorService buySellTradingHistoricalSimulatorService = new com.ukora.tradestudent.services.simulator.origin.BuySellTradingHistoricalSimulatorService()

    @Unroll
    def "test simulateTrade for accuracy #scenario"() {

        given:
        String purseKey = 'purseKeyValue'
        Simulation simulation = new Simulation()
        simulation.balancesA.put(purseKey, balanceA)
        simulation.balancesB.put(purseKey, balanceB)
        simulation.transactionCost = transactionCost
        TradeExecution tradeExecution = new TradeExecution()
        tradeExecution.price = 10
        tradeExecution.date = new Date()
        tradeExecution.amount = amount
        tradeExecution.tradeType = tradeType


        when:
        com.ukora.tradestudent.services.simulator.origin.BuySellTradingHistoricalSimulatorService.simulateTrade(
                simulation,
                tradeExecution,
                purseKey
        )

        then:
        simulation.balancesA.get(purseKey) == finalBalanceA
        simulation.balancesB.get(purseKey) == finalBalanceB

        where:
        scenario                                            | tradeType                     | amount | balanceA | balanceB | finalBalanceA | finalBalanceB | transactionCost
        "Selling a negative amount is ignored"              | TradeExecution.TradeType.SELL | -2     | 1        | 0        | 1             | 0             | 0
        "Buying a negative amount is ignored"               | TradeExecution.TradeType.BUY  | -2     | 1        | 0        | 1             | 0             | 0
        "Selling more than available"                       | TradeExecution.TradeType.SELL | 2      | 1        | 0        | 0             | 10            | 0
        "Buying more than available"                        | TradeExecution.TradeType.BUY  | 2      | 1        | 10       | 2             | 0             | 0
        "Buying with transaction cost"                      | TradeExecution.TradeType.BUY  | 1      | 0        | 10       | 0.998         | 0             | 0.002
        "Selling with transaction cost"                     | TradeExecution.TradeType.SELL | 1      | 1        | 0        | 0             | 9.98          | 0.002
        "Buying more than available with transaction cost"  | TradeExecution.TradeType.BUY  | 2      | 0        | 10       | 0.998         | 0             | 0.002
        "Selling more than available with transaction cost" | TradeExecution.TradeType.SELL | 2      | 1        | 0        | 0             | 9.98          | 0.002

    }

}
