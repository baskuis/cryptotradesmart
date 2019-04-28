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

        Double buyProbobability = correlationAssociation.tagProbabilities.get(combinerStrategy)?.get(buyTag.tagName) - 0.5
        Double upProbobability = correlationAssociation.tagProbabilities.get(combinerStrategy)?.get(upTag.tagName) - 0.5
        Double upReversalProbobability = correlationAssociation.tagProbabilities.get(combinerStrategy)?.get(upReversalTag.tagName) - 0.5
        Double upMoveProbobability = correlationAssociation.tagProbabilities.get(combinerStrategy)?.get(upMoveTag.tagName) - 0.5

        Double aggregateBuyProbability =
                (
                    (buyProbobability * simulation.tagGroupWeights.get(buySellTagGroup.name)) +
                    (upProbobability * simulation.tagGroupWeights.get(upDownTagGroup.name)) +
                    (upReversalProbobability * simulation.tagGroupWeights.get(upDownReversalTagGroup.name)) +
                    (upMoveProbobability * simulation.tagGroupWeights.get(upDownMovesTagGroup.name))
                ) / 4
        Double aggregateSellProbability = 1 - aggregateBuyProbability
        if (aggregateBuyProbability > simulation.buyThreshold) {
            tradeExecution = new TradeExecution(
                    tradeType: TradeExecution.TradeType.BUY,
                    amount: simulation.tradeIncrement,
                    price: correlationAssociation.price,
                    date: correlationAssociation.date
            )
        } else if ( aggregateSellProbability > simulation.sellThreshold) {
            tradeExecution = new TradeExecution(
                    tradeType: TradeExecution.TradeType.SELL,
                    amount: simulation.tradeIncrement,
                    price: correlationAssociation.price,
                    date: correlationAssociation.date
            )
        }
        return tradeExecution
    }

}
