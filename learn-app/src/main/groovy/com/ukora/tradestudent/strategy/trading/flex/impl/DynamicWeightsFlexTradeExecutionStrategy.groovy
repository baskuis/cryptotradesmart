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
import com.ukora.tradestudent.services.simulator.Simulation
import com.ukora.tradestudent.strategy.trading.flex.FlexTradeExecutionStrategy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * This trade execution strategy combines tag correlations according to variable weights
 *
 */
@Component
class DynamicWeightsFlexTradeExecutionStrategy implements FlexTradeExecutionStrategy {

    @Autowired BuySellTagGroup buySellTagGroup
    @Autowired UpDownTagGroup upDownTagGroup
    @Autowired UpDownReversalTagGroup upDownReversalTagGroup
    @Autowired UpDownMovesTagGroup upDownMovesTagGroup

    @Autowired BuyTag buyTag
    @Autowired UpTag upTag
    @Autowired UpReversalTag upReversalTag
    @Autowired UpMoveTag upMoveTag

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
        return "dyno"
    }

    @Override
    TradeExecution getTrade(CorrelationAssociation correlationAssociation, Simulation simulation, String combinerStrategy, Double balanceProportion) {
        TradeExecution tradeExecution = null
        Double buyProbobability = correlationAssociation.tagProbabilities.get(combinerStrategy)?.get(buyTag.tagName)
        Double upProbobability = correlationAssociation.tagProbabilities.get(combinerStrategy)?.get(upTag.tagName)
        Double upReversalProbobability = correlationAssociation.tagProbabilities.get(combinerStrategy)?.get(upReversalTag.tagName)
        Double upMoveProbobability = correlationAssociation.tagProbabilities.get(combinerStrategy)?.get(upMoveTag.tagName)
        if(buyProbobability && upProbobability && upReversalProbobability && upMoveProbobability) {
            Double buy = buyProbobability - 0.5d
            Double up = upProbobability - 0.5d
            Double upReversal = upReversalProbobability - 0.5d
            Double upMove = upMoveProbobability - 0.5d
            Double aggregateBuyProbability =
                    (
                            (buy * simulation.tagGroupWeights.get(buySellTagGroup.name)) +
                                    (up * simulation.tagGroupWeights.get(upDownTagGroup.name)) +
                                    (upReversal * simulation.tagGroupWeights.get(upDownReversalTagGroup.name)) +
                                    (upMove * simulation.tagGroupWeights.get(upDownMovesTagGroup.name))
                    ) / 4
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
