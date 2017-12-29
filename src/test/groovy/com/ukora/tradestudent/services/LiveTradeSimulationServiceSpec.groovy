package com.ukora.tradestudent.services

import com.ukora.tradestudent.entities.SimulatedTradeEntry
import com.ukora.tradestudent.strategy.trading.TradeExecution
import spock.lang.Specification
import spock.lang.Unroll

class LiveTradeSimulationServiceSpec extends Specification {

    LiveTradeSimulationService liveTradeSimulationService = new LiveTradeSimulationService()

    @Unroll
    def "test that getNextSimulatedTradeEntry #tradeType #scenario produces #expected"() {

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
        if(!expected){
            null == r
        } else {
            r.totalValueA == expected
            r.balanceB == expectedB
            r.balanceA == expectedA
        }

        where:
        scenario               | tradeType                     | amount | price | balanceA | balanceB | expected | expectedA | expectedB
        "Invalid amount"       | TradeExecution.TradeType.BUY  | 0      | 5     | 10       | 0        | null     | 10.0      | 0
        "Valid buy"            | TradeExecution.TradeType.BUY  | 0.5    | 10000 | 10       | 6000     | 10.599   | 10.499    | 1000
        "Invalid amount"       | TradeExecution.TradeType.SELL | 0      | 5     | 10       | 0        | null     | 10.0      | 0
        "Valid sell"           | TradeExecution.TradeType.SELL | 0.1    | 10000 | 10       | 6000     | 10.5998  | 9.9       | 6998.0
        "Insufficient balance" | TradeExecution.TradeType.BUY  | 5      | 10000 | 1        | 0        | null     | 1         | 0.0
        "Insufficient balance" | TradeExecution.TradeType.BUY  | 5      | 10000 | 1        | 1000     | 1.0998   | 1.0998    | 0.0
        "Insufficient balance" | TradeExecution.TradeType.SELL | 5      | 10000 | 1        | 1000     | 1.098    | 0.0       | 10980.0

    }

}
