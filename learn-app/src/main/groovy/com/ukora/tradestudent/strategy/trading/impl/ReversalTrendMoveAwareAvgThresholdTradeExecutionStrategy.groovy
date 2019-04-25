package com.ukora.tradestudent.strategy.trading.impl

import com.ukora.domain.beans.tags.TagSubset
import com.ukora.domain.beans.tags.buysell.BuySellTagGroup
import com.ukora.domain.beans.tags.buysell.BuyTag
import com.ukora.domain.beans.tags.buysell.SellTag
import com.ukora.domain.beans.tags.moves.UpMoveTag
import com.ukora.domain.beans.tags.reversal.DownReversalTag
import com.ukora.domain.beans.tags.reversal.UpReversalTag
import com.ukora.domain.beans.tags.trend.DownTag
import com.ukora.domain.beans.tags.trend.UpTag
import com.ukora.domain.beans.trade.TradeExecution
import com.ukora.domain.entities.CorrelationAssociation
import com.ukora.tradestudent.services.simulator.Simulation
import com.ukora.tradestudent.strategy.trading.TradeExecutionStrategy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * This strategy uses an average of buy, up, up-reversal, up-move
 *
 */
@Component
class ReversalTrendMoveAwareAvgThresholdTradeExecutionStrategy implements TradeExecutionStrategy, TagSubset {

    @Autowired
    BuyTag buyTag

    @Autowired
    SellTag sellTag

    @Autowired
    UpTag upTag

    @Autowired
    DownTag downTag

    @Autowired
    UpMoveTag upMoveTag

    @Autowired
    BuySellTagGroup buySellTagGroup

    @Autowired
    UpReversalTag upReversalTag

    @Autowired
    DownReversalTag downReversalTag

    @Override
    boolean applies(String toTag) {
        return buySellTagGroup.applies(toTag)
    }

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
        return "bower"
    }

    /**
     * Execute trades while taking reversal probabilities into account
     *
     * @param correlationAssociation
     * @param tag
     * @param probability
     * @param simulation
     * @param combinerStrategy
     * @param balanceProportion
     * @return
     */
    @Override
    TradeExecution getTrade(
            CorrelationAssociation correlationAssociation,
            String tag,
            Double probability,
            Simulation simulation,
            String combinerStrategy,
            Double balanceProportion
    ) {
        TradeExecution tradeExecution = null
        Double buyProbability = correlationAssociation.tagProbabilities?.get(combinerStrategy)?.get(buyTag.tagName)
        Double upProbability = correlationAssociation.tagProbabilities?.get(combinerStrategy)?.get(upTag.tagName)
        Double upReversalProbability = correlationAssociation.tagProbabilities?.get(combinerStrategy)?.get(upReversalTag.tagName)
        Double upMoveProbability = correlationAssociation.tagProbabilities?.get(combinerStrategy)?.get(upMoveTag.tagName)
        if (buyProbability && upProbability && upReversalProbability && upMoveProbability) {
            Double upAvg = (buyProbability + upProbability + upReversalProbability + upMoveProbability) / 4
            Double downAvg = 1 - upAvg
            if (tag == buyTag.getTagName() && upAvg > simulation.buyThreshold) {
                tradeExecution = new TradeExecution(
                        tradeType: TradeExecution.TradeType.BUY,
                        amount: simulation.tradeIncrement,
                        price: correlationAssociation.price,
                        date: correlationAssociation.date
                )
            } else if (tag == sellTag.getTagName() && downAvg > simulation.sellThreshold) {
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
