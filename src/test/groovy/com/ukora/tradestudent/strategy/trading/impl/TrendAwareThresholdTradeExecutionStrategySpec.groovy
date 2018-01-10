package com.ukora.tradestudent.strategy.trading.impl

import com.ukora.tradestudent.entities.CorrelationAssociation
import com.ukora.tradestudent.services.simulator.Simulation
import com.ukora.tradestudent.strategy.trading.TradeExecution
import com.ukora.tradestudent.tags.buysell.BuyTag
import com.ukora.tradestudent.tags.buysell.SellTag
import com.ukora.tradestudent.tags.trend.DownTag
import com.ukora.tradestudent.tags.trend.UpTag
import spock.lang.Specification
import spock.lang.Unroll


class TrendAwareThresholdTradeExecutionStrategySpec extends Specification {

    TrendAwareThresholdTradeExecutionStrategy tradeExecutionStrategy

    def setup() {
        tradeExecutionStrategy = new TrendAwareThresholdTradeExecutionStrategy(
                upTag: new UpTag(),
                downTag: new DownTag(),
                buyTag: new BuyTag(),
                sellTag: new SellTag()
        )
    }

    @Unroll
    def "test getTrade #tradeType #scenario with p: #probability produces amount: #finalAmount"() {

        setup:
        CorrelationAssociation correlationAssociation = new CorrelationAssociation(
                tagProbabilities: [
                        relevanceWeightedProbabilityCombinerStrategy: [
                                buy : buyProbability,
                                sell: sellProbablity,
                                up  : upProbability,
                                down: downProbability
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
                combinerStrategy
        )

        then:
        if (!finalAmount) assert !tradeExecution
        if (finalAmount) {
            assert tradeExecution.tradeType == tradeType
            assert tradeExecution.amount == finalAmount
        }

        where:
        scenario                               | tag    | tradeType                     | amount | buyThreshold | sellThreshold | combinerStrategy                     | buyProbability | sellProbablity | upProbability | downProbability | probability | finalAmount
        "Up likely lowers buy threshold"       | "buy"  | TradeExecution.TradeType.BUY  | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.6d           | 0.4d           | 0.6d          | 0.4d            | 0.46d       | 0.3
        "Up likely lowers buy threshold"       | "buy"  | TradeExecution.TradeType.BUY  | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.6d           | 0.4d           | 0.6d          | 0.4d            | 0.44d       | null
        "Up/Down balances keeps buy threshold" | "buy"  | TradeExecution.TradeType.BUY  | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.6d           | 0.4d           | 0.5d          | 0.5d            | 0.51d       | 0.3
        "Up/Down balances keeps buy threshold" | "buy"  | TradeExecution.TradeType.BUY  | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.6d           | 0.4d           | 0.5d          | 0.5d            | 0.49d       | null
        "Down likely lowers buy threshold"     | "sell" | TradeExecution.TradeType.SELL | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.4d           | 0.6d           | 0.4d          | 0.6d            | 0.46d       | 0.3
        "Down likely lowers buy threshold"     | "sell" | TradeExecution.TradeType.SELL | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.4d           | 0.6d           | 0.4d          | 0.6d            | 0.44d       | null

    }

}
