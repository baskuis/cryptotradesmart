package com.ukora.tradestudent.strategy.trading.impl

import com.ukora.domain.entities.CorrelationAssociation
import com.ukora.domain.entities.ExtractedText
import com.ukora.tradestudent.services.simulator.Simulation
import com.ukora.tradestudent.services.text.TextAssociationProbabilityService
import com.ukora.domain.beans.trade.TradeExecution
import com.ukora.tradestudent.strategy.trading.TradeExecutionStrategy
import com.ukora.domain.beans.tags.buysell.BuyTag
import com.ukora.domain.beans.tags.buysell.SellTag
import com.ukora.domain.beans.tags.reversal.DownReversalTag
import com.ukora.domain.beans.tags.reversal.UpReversalTag
import com.ukora.domain.beans.tags.trend.DownTag
import com.ukora.domain.beans.tags.trend.UpTag
import spock.lang.Specification
import spock.lang.Unroll

/**
 * TODO: Review results
 *
 */
class NewsBalanceReversalTrendAwareProgressiveTradeExecutionStrategySpec extends Specification {

    TradeExecutionStrategy tradeExecutionStrategy

    TextAssociationProbabilityService mockTextAssociationProbabilityService = Mock(TextAssociationProbabilityService)

    def setup() {

        tradeExecutionStrategy = new NewsBalanceReversalTrendAwareProgressiveTradeExecutionStrategy(
                upTag: new UpTag(),
                downTag: new DownTag(),
                buyTag: new BuyTag(),
                sellTag: new SellTag(),
                upReversalTag: new UpReversalTag(),
                downReversalTag: new DownReversalTag(),
                textAssociationProbabilityService: mockTextAssociationProbabilityService
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
                                up          : upProbability,
                                down        : downProbability,
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
                balanceAmount
        )

        then:
        1 * mockTextAssociationProbabilityService.tagCorrelationByText(_) >> [
                averageTextProbabilityCombinerStrategy: [
                        (ExtractedText.TextSource.NEWS.name()): [
                                'buy'         : 0.60,
                                'sell'        : 0.40,
                                'up'          : 0.55,
                                'down'        : 0.45,
                                'upreversal'  : 0.55,
                                'downreversal': 0.45
                        ]
                ]
        ]
        if (!finalAmount) assert !tradeExecution
        if (finalAmount) {
            assert tradeExecution?.tradeType == tradeType
            assert tradeExecution?.amount == finalAmount
        }

        where:
        scenario                                    | tag    | tradeType                     | amount | buyThreshold | sellThreshold | combinerStrategy                     | buyProbability | sellProbablity | upProbability | downProbability | upReversalProbability | downReversalProbability | probability | balanceAmount | finalAmount
        "Up likely lowers buy threshold"            | "buy"  | TradeExecution.TradeType.BUY  | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.6d           | 0.4d           | 0.6d          | 0.4d            | 0.6d                  | 0.4d                    | 0.46d       | 0.5           | 0.0748825065274152
        "Up likely lowers buy threshold"            | "buy"  | TradeExecution.TradeType.BUY  | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.6d           | 0.4d           | 0.6d          | 0.4d            | 0.6d                  | 0.4d                    | 0.44d       | 0.5           | null
        "Up/Down balances keeps buy threshold"      | "buy"  | TradeExecution.TradeType.BUY  | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.6d           | 0.4d           | 0.5d          | 0.5d            | 0.5d                  | 0.5d                    | 0.51d       | 0.5           | 0.0922946175637394
        "Up/Down balances keeps buy threshold"      | "buy"  | TradeExecution.TradeType.BUY  | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.6d           | 0.4d           | 0.5d          | 0.5d            | 0.5d                  | 0.5d                    | 0.49d       | 0.5           | null
        "Down likely lowers buy threshold"          | "sell" | TradeExecution.TradeType.SELL | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.4d           | 0.6d           | 0.4d          | 0.6d            | 0.4d                  | 0.6d                    | 0.46d       | 0.5           | null
        "Down likely lowers buy threshold"          | "sell" | TradeExecution.TradeType.SELL | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.4d           | 0.6d           | 0.4d          | 0.6d            | 0.4d                  | 0.6d                    | 0.44d       | 0.5           | null
        "Down likely, sell likely increases amount" | "sell" | TradeExecution.TradeType.SELL | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.2d           | 0.8d           | 0.4d          | 0.6d            | 0.4d                  | 0.6d                    | 0.80d       | 0.5           | 0.7766578249336871
        "Down likely, sell likely increases amount" | "sell" | TradeExecution.TradeType.SELL | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.1d           | 0.9d           | 0.4d          | 0.6d            | 0.4d                  | 0.6d                    | 0.90d       | 0.5           | 0.9883289124668435
        "Down likely, sell likely increases amount" | "sell" | TradeExecution.TradeType.SELL | 0.3    | 0.5          | 0.5           | "averageProbabilityCombinerStrategy" | 0.0d           | 1.0d           | 0.4d          | 0.6d            | 0.4d                  | 0.6d                    | 1.00d       | 0.5           | 1.2

    }

}
