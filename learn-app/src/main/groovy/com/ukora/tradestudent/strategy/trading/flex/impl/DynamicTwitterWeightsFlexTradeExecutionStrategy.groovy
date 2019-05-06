package com.ukora.tradestudent.strategy.trading.flex.impl

import com.ukora.domain.beans.tags.buysell.BuySellTagGroup
import com.ukora.domain.beans.tags.buysell.BuyTag
import com.ukora.domain.beans.tags.moves.UpDownMovesTagGroup
import com.ukora.domain.beans.tags.moves.UpMoveTag
import com.ukora.domain.beans.tags.reversal.UpDownReversalTagGroup
import com.ukora.domain.beans.tags.reversal.UpReversalTag
import com.ukora.domain.beans.tags.trend.UpDownTagGroup
import com.ukora.domain.beans.tags.trend.UpTag
import com.ukora.domain.beans.trade.TradeExecution
import com.ukora.domain.entities.CorrelationAssociation
import com.ukora.domain.entities.TextCorrelationAssociation
import com.ukora.tradestudent.services.simulator.Simulation
import com.ukora.tradestudent.strategy.trading.flex.FlexTradeExecutionStrategy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DynamicTwitterWeightsFlexTradeExecutionStrategy implements FlexTradeExecutionStrategy {

    final static String COMBINER_STRATEGY = 'averageTextProbabilityCombinerStrategy'

    @Autowired
    BuySellTagGroup buySellTagGroup
    @Autowired
    UpDownTagGroup upDownTagGroup
    @Autowired
    UpDownReversalTagGroup upDownReversalTagGroup
    @Autowired
    UpDownMovesTagGroup upDownMovesTagGroup

    @Autowired
    BuyTag buyTag
    @Autowired
    UpTag upTag
    @Autowired
    UpReversalTag upReversalTag
    @Autowired
    UpMoveTag upMoveTag

    private boolean enabled = true

    @Override
    boolean isEnabled() {
        return enabled
    }

    @Override
    void setEnabled(boolean enabled) {
        this.enabled = enabled
    }

    @Override
    String getAlias() {
        return "willem"
    }

    @Override
    TradeExecution getTrade(
            CorrelationAssociation correlationAssociation,
            TextCorrelationAssociation textCorrelationAssociation,
            Simulation simulation,
            String combinerStrategy,
            Double balanceProportion
    ) {
        TradeExecution tradeExecution = null
        Double buyProbability = textCorrelationAssociation.strategyProbabilities?.get(COMBINER_STRATEGY)?.TWITTER?.get(buyTag.tagName)
        Double upProbability = textCorrelationAssociation.strategyProbabilities?.get(COMBINER_STRATEGY)?.TWITTER?.get(upTag.tagName)
        Double upReversalProbability = textCorrelationAssociation.strategyProbabilities?.get(COMBINER_STRATEGY)?.TWITTER?.get(upReversalTag.tagName)
        Double upMoveProbability = textCorrelationAssociation.strategyProbabilities?.get(COMBINER_STRATEGY)?.TWITTER?.get(upMoveTag.tagName)
        if (buyProbability && upProbability && upReversalProbability && upMoveProbability) {
            Double buy = buyProbability - 0.5d
            Double up = upProbability - 0.5d
            Double upReversal = upReversalProbability - 0.5d
            Double upMove = upMoveProbability - 0.5d
            Double buyWeight = simulation.tagGroupWeights.get(buySellTagGroup.name)
            Double upWeight = simulation.tagGroupWeights.get(upDownTagGroup.name)
            Double upReversalWeight = simulation.tagGroupWeights.get(upDownReversalTagGroup.name)
            Double upMoveWeight = simulation.tagGroupWeights.get(upDownMovesTagGroup.name)
            Double aggregateBuyProbability =
                    0.5d + (
                            (
                                    (buy * buyWeight) +
                                            (up * upWeight) +
                                            (upReversal * upReversalWeight) +
                                            (upMove * upMoveWeight)
                            ) / (Math.abs(buyWeight) + Math.abs(upWeight) + Math.abs(upReversalWeight) + Math.abs(upMoveWeight))
                    )
            Double aggregateSellProbability = 1 - aggregateBuyProbability
            if (aggregateBuyProbability > simulation.buyThreshold) {
                tradeExecution = new TradeExecution(
                        tradeType: TradeExecution.TradeType.BUY,
                        amount: simulation.tradeIncrement,
                        price: correlationAssociation.price,
                        date: correlationAssociation.date
                )
            } else if (aggregateSellProbability > simulation.sellThreshold) {
                tradeExecution = new TradeExecution(
                        tradeType: TradeExecution.TradeType.SELL,
                        amount: simulation.tradeIncrement,
                        price: correlationAssociation.price,
                        date: correlationAssociation.date
                )
            }
        }
        return tradeExecution
    }

}

