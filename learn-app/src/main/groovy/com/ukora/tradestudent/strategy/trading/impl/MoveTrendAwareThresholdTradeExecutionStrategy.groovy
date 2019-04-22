package com.ukora.tradestudent.strategy.trading.impl

import com.ukora.domain.beans.tags.TagSubset
import com.ukora.domain.beans.tags.buysell.BuySellTagGroup
import com.ukora.domain.beans.tags.buysell.BuyTag
import com.ukora.domain.beans.tags.buysell.SellTag
import com.ukora.domain.beans.tags.moves.DownMoveTag
import com.ukora.domain.beans.tags.moves.UpMoveTag
import com.ukora.domain.beans.tags.trend.DownTag
import com.ukora.domain.beans.tags.trend.UpTag
import com.ukora.domain.beans.trade.TradeExecution
import com.ukora.domain.entities.CorrelationAssociation
import com.ukora.tradestudent.services.simulator.Simulation
import com.ukora.tradestudent.strategy.trading.TradeExecutionStrategy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class MoveTrendAwareThresholdTradeExecutionStrategy implements TradeExecutionStrategy, TagSubset {

    @Autowired
    BuyTag buyTag

    @Autowired
    SellTag sellTag

    @Autowired
    BuySellTagGroup buySellTagGroup

    @Autowired
    UpMoveTag upMoveTag

    @Autowired
    DownMoveTag downMoveTag

    @Autowired
    UpTag upTag

    @Autowired
    DownTag downTag

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
        return "jack"
    }

    /**
     * Execute trades while taking trend probabilities into account
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
        Double upProbability = correlationAssociation.tagProbabilities?.get(combinerStrategy)?.get(upTag.tagName)
        Double downProbability = correlationAssociation.tagProbabilities?.get(combinerStrategy)?.get(downTag.tagName)
        Double upMoveProbability = correlationAssociation.tagProbabilities?.get(combinerStrategy)?.get(upMoveTag.tagName)
        Double downMoveProbability = correlationAssociation.tagProbabilities?.get(combinerStrategy)?.get(downMoveTag.tagName)
        if (upProbability && downProbability && upMoveProbability && downMoveProbability) {
            Double modifiedBuyThreshold
            Double modifiedSellThreshold
            if (upProbability > downProbability) {
                Double trendDelta = upProbability - downProbability
                Double moveDelta = upMoveProbability - downMoveProbability
                modifiedBuyThreshold =
                        simulation.buyThreshold -
                                (trendDelta * (1 - simulation.buyThreshold) / 2) -
                                (moveDelta * (1 - simulation.buyThreshold) / 2)
                modifiedSellThreshold =
                        simulation.sellThreshold +
                                (trendDelta * (1 - simulation.sellThreshold) / 2) +
                                (moveDelta * (1 - simulation.sellThreshold) / 2)
            } else {
                Double trendDelta = downProbability - upProbability
                Double moveDelta = downMoveProbability - upMoveProbability
                modifiedBuyThreshold =
                        simulation.buyThreshold +
                                (trendDelta * (1 - simulation.buyThreshold) / 2) +
                                (moveDelta * (1 - simulation.buyThreshold) / 2)
                modifiedSellThreshold =
                        simulation.sellThreshold -
                                (trendDelta * (1 - simulation.sellThreshold) / 2) -
                                (moveDelta * (1 - simulation.sellThreshold) / 2)
            }
            if (tag == buyTag.getTagName() && probability > modifiedBuyThreshold) {
                tradeExecution = new TradeExecution(
                        tradeType: TradeExecution.TradeType.BUY,
                        amount: simulation.tradeIncrement,
                        price: correlationAssociation.price,
                        date: correlationAssociation.date
                )
            } else if (tag == sellTag.getTagName() && probability > modifiedSellThreshold) {
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

