package com.ukora.tradestudent.strategy.trading.impl

import com.ukora.domain.beans.tags.buysell.BuyTag
import com.ukora.domain.beans.tags.buysell.SellTag
import com.ukora.domain.beans.tags.moves.DownMoveTag
import com.ukora.domain.beans.tags.moves.UpMoveTag
import com.ukora.domain.beans.tags.trend.DownTag
import com.ukora.domain.beans.tags.trend.UpTag
import com.ukora.domain.beans.trade.TradeExecution
import com.ukora.domain.entities.CorrelationAssociation
import com.ukora.tradestudent.services.simulator.Simulation
import spock.lang.Specification
import spock.lang.Unroll


class MoveTrendAwareThresholdTradeExecutionStrategySpec extends Specification {

    MoveTrendAwareThresholdTradeExecutionStrategy tradeExecutionStrategy

    def setup() {
        tradeExecutionStrategy = new MoveTrendAwareThresholdTradeExecutionStrategy(
                upTag: new UpTag(),
                downTag: new DownTag(),
                buyTag: new BuyTag(),
                sellTag: new SellTag(),
                upMoveTag: new UpMoveTag(),
                downMoveTag: new DownMoveTag()
        )
    }

    @Unroll
    def "test getTrade #tradeType #scenario with p: #probability produces amount: #finalAmount"() {

        setup:
        CorrelationAssociation correlationAssociation = new CorrelationAssociation(
                tagProbabilities: [
                        averageProbabilityCombinerStrategy: [
                                buy     : buyProbability,
                                sell    : sellProbablity,
                                up      : upProbability,
                                down    : downProbability,
                                upmove  : upMoveProbability,
                                downmove: downMoveProbability
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
        scenario                              | tag   | tradeType                    | amount | buyThreshold | sellThreshold | combinerStrategy                     | buyProbability | sellProbablity | upProbability | downProbability | upMoveProbability | downMoveProbability | probability | finalAmount
        "Up likely lowers buy threshold"       | "buy"  | TradeExecution.TradeType.BUY  | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.6d           | 0.4d           | 0.6d          | 0.4d            | 0.5d              | 0.5d                | 0.46d       | 0.3
        "Up likely lowers buy threshold"      | "buy" | TradeExecution.TradeType.BUY | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.6d           | 0.4d           | 0.6d          | 0.4d            | 0.5d              | 0.5d                | 0.44d       | null
        "Up Move likely lowers buy threshold" | "buy" | TradeExecution.TradeType.BUY | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.6d           | 0.4d           | 0.6d          | 0.4d            | 0.7d              | 0.3d                | 0.44d       | 0.3
        "Up/Down balances keeps buy threshold" | "buy"  | TradeExecution.TradeType.BUY  | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.6d           | 0.4d           | 0.5d          | 0.5d            | 0.5d              | 0.5d                | 0.51d       | 0.3
        "Up/Down balances keeps buy threshold" | "buy"  | TradeExecution.TradeType.BUY  | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.6d           | 0.4d           | 0.5d          | 0.5d            | 0.5d              | 0.5d                | 0.49d       | null
        "Down likely lowers buy threshold"     | "sell" | TradeExecution.TradeType.SELL | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.4d           | 0.6d           | 0.4d          | 0.6d            | 0.5d              | 0.5d                | 0.46d       | 0.3
        "Down likely lowers buy threshold"     | "sell" | TradeExecution.TradeType.SELL | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.4d           | 0.6d           | 0.4d          | 0.6d            | 0.5d              | 0.5d                | 0.44d       | null

    }

}
