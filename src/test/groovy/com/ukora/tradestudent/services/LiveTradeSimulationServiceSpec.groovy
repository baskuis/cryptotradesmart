package com.ukora.tradestudent.services

import com.ukora.tradestudent.entities.SimulatedTradeEntry
import com.ukora.tradestudent.strategy.trading.TradeExecution
import spock.lang.Specification
import spock.lang.Unroll

class LiveTradeSimulationServiceSpec extends Specification {

    LiveTradeSimulationService liveTradeSimulationService = new LiveTradeSimulationService()

    @Unroll
    def "test that getNextSimulatedTradeEntry #scenario produces #expected"() {

        setup:
        SimulatedTradeEntry simulatedTradeEntry = new SimulatedTradeEntry(
                balanceA: balanceA,
                balanceB: balanceB
        )
        TradeExecution tradeExecution = new TradeExecution(
                tradeType: tradeType,
                amount: amount,
                price: price
        )

        when:
        SimulatedTradeEntry r = liveTradeSimulationService.getNextSimulatedTradeEntry(simulatedTradeEntry, tradeExecution)

        then:
        r.totalValueA == expected

        where:
        scenario         | tradeType                     | amount | price | balanceA | balanceB | expected
        "Invalid amount" | TradeExecution.TradeType.BUY  | 0      | 5     | 10       | 0        | 10
        "Valid buy"      | TradeExecution.TradeType.BUY  | 0.5    | 10000 | 10       | 2000     | 10.199
        "Invalid amount" | TradeExecution.TradeType.SELL | 0      | 5     | 10       | 0        | 10
        "Valid sell"     | TradeExecution.TradeType.SELL | 0.1    | 10000 | 10       | 2000     | 10.1998

    }

}
