package com.ukora.tradestudent.strategy.trading.impl

import com.ukora.tradestudent.entities.CorrelationAssociation
import com.ukora.tradestudent.services.simulator.Simulation
import com.ukora.tradestudent.strategy.trading.TradeExecution
import com.ukora.tradestudent.tags.buysell.BuyTag
import com.ukora.tradestudent.tags.buysell.SellTag
import spock.lang.Specification
import spock.lang.Unroll

class ProgressiveV1ThresholdTradeExecutionStrategySpec extends Specification {

    ProgressiveV1ThresholdTradeExecutionStrategy tradeExecutionStrategy

    def setup() {
        tradeExecutionStrategy = new ProgressiveV1ThresholdTradeExecutionStrategy(
                buyTag: new BuyTag(),
                sellTag: new SellTag()
        )
    }

    @Unroll
    def "test getTrade #tradeType #scenario with p: #probability produces amount: #finalAmount"() {

        setup:
        CorrelationAssociation correlationAssociation = new CorrelationAssociation(
                tagProbabilities: [
                        averageProbabilityCombinerStrategy: [
                                buy : buyProbability,
                                sell: sellProbablity
                        ]
                ]
        )
        Simulation simulation = new Simulation(
                tradeIncrement: amount,
                buyThreshold: buyThreshold,
                sellThreshold: sellThreshold
        )


        when:
        TradeExecution tradeExecution = tradeExecutionStrategy.getTrade(
                correlationAssociation,
                tag,
                probability,
                simulation,
                combinerStrategy,
                0.5
        )

        then:
        if (!finalAmount) assert !tradeExecution
        if (finalAmount) {
            assert tradeExecution.tradeType == tradeType
            assert tradeExecution.amount == finalAmount
        }

        where:
        scenario                            | tag    | tradeType                     | amount | buyThreshold | sellThreshold | combinerStrategy                     | buyProbability | sellProbablity | probability | finalAmount
        "Low probability lowers amount"     | "buy"  | TradeExecution.TradeType.BUY  | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.6d           | 0.4d           | 0.6d        | 0.2639999999999999
        "High probability increases amount" | "buy"  | TradeExecution.TradeType.BUY  | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.9d           | 0.1d           | 0.9d        | 0.966
        "Low probability lowers amoun"      | "sell" | TradeExecution.TradeType.SELL | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.4d           | 0.6d           | 0.6d        | 0.2639999999999999
        "High probability increases amount" | "sell" | TradeExecution.TradeType.SELL | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.1d           | 0.9d           | 0.9d        | 0.966

    }

}
