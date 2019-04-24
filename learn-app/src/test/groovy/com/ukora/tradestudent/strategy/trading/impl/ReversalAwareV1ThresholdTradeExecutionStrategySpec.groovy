package com.ukora.tradestudent.strategy.trading.impl

import com.ukora.domain.beans.tags.buysell.BuyTag
import com.ukora.domain.beans.tags.buysell.SellTag
import com.ukora.domain.beans.tags.reversal.DownReversalTag
import com.ukora.domain.beans.tags.reversal.UpReversalTag
import com.ukora.domain.beans.trade.TradeExecution
import com.ukora.domain.entities.CorrelationAssociation
import com.ukora.tradestudent.services.simulator.Simulation
import com.ukora.tradestudent.strategy.trading.TradeExecutionStrategy
import spock.lang.Specification
import spock.lang.Unroll

class ReversalAwareV1ThresholdTradeExecutionStrategySpec extends Specification {

    TradeExecutionStrategy tradeExecutionStrategy

    def setup() {
        tradeExecutionStrategy = new ReversalAwareV1ThresholdTradeExecutionStrategy(
                buyTag: new BuyTag(),
                sellTag: new SellTag(),
                upReversalTag: new UpReversalTag(),
                downReversalTag: new DownReversalTag()
        )
    }

    @Unroll
    def "test getTrade #tradeType #scenario with p: #probability produces amount: #finalAmount"() {

        setup:
        CorrelationAssociation correlationAssociation = new CorrelationAssociation(
                tagProbabilities: [
                        averageProbabilityCombinerStrategy: [
                                buy         : buyProbability,
                                sell        : sellProbablity,
                                upreversal  : upReversalProbability,
                                downreversal: downReversalProbability
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
        scenario                                    | tag    | tradeType                     | amount | buyThreshold | sellThreshold | combinerStrategy                     | buyProbability | sellProbablity | upReversalProbability | downReversalProbability | probability | finalAmount
        "Up likely lowers buy threshold"            | "buy"  | TradeExecution.TradeType.BUY  | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.6d           | 0.4d           | 0.6d                  | 0.4d                    | 0.66d       | 0.3
        "Up likely lowers buy threshold"            | "buy"  | TradeExecution.TradeType.BUY  | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.6d           | 0.4d           | 0.6d                  | 0.4d                    | 0.44d       | null
        "Up/Down balances keeps buy threshold"      | "buy"  | TradeExecution.TradeType.BUY  | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.6d           | 0.4d           | 0.5d                  | 0.5d                    | 0.51d       | 0.3
        "Up/Down balances keeps buy threshold"      | "buy"  | TradeExecution.TradeType.BUY  | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.6d           | 0.4d           | 0.5d                  | 0.5d                    | 0.49d       | null
        "Down likely lowers buy threshold"          | "sell" | TradeExecution.TradeType.SELL | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.4d           | 0.6d           | 0.4d                  | 0.6d                    | 0.46d       | null
        "Down likely lowers buy threshold"          | "sell" | TradeExecution.TradeType.SELL | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.4d           | 0.6d           | 0.4d                  | 0.6d                    | 0.44d       | null
        "Down likely, sell likely increases amount" | "sell" | TradeExecution.TradeType.SELL | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.2d           | 0.8d           | 0.4d                  | 0.6d                    | 0.80d       | 0.3
        "Down likely, sell likely increases amount" | "sell" | TradeExecution.TradeType.SELL | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.1d           | 0.9d           | 0.4d                  | 0.6d                    | 0.90d       | 0.3
        "Down likely, sell likely increases amount" | "sell" | TradeExecution.TradeType.SELL | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.0d           | 1.0d           | 0.4d                  | 0.6d                    | 1.00d       | 0.3

    }

}
