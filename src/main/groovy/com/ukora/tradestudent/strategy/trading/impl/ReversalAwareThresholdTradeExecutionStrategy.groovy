package com.ukora.tradestudent.strategy.trading.impl

import com.ukora.tradestudent.entities.CorrelationAssociation
import com.ukora.tradestudent.services.simulator.Simulation
import com.ukora.tradestudent.strategy.trading.TradeExecution
import com.ukora.tradestudent.strategy.trading.TradeExecutionStrategy
import com.ukora.tradestudent.tags.TagSubset
import com.ukora.tradestudent.tags.buysell.BuySellTagGroup
import com.ukora.tradestudent.tags.buysell.BuyTag
import com.ukora.tradestudent.tags.buysell.SellTag
import com.ukora.tradestudent.tags.reversal.DownReversalTag
import com.ukora.tradestudent.tags.reversal.UpReversalTag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ReversalAwareThresholdTradeExecutionStrategy implements TradeExecutionStrategy, TagSubset {

    @Autowired
    BuyTag buyTag

    @Autowired
    SellTag sellTag

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
        return "erin"
    }

    /**
     * Execute trades while taking reversal probabilities into account
     *
     * @param correlationAssociation
     * @param tag
     * @param probability
     * @param simulation
     * @param combinerStrategy
     * @return
     */
    @Override
    TradeExecution getTrade(
            CorrelationAssociation correlationAssociation,
            String tag,
            Double probability,
            Simulation simulation,
            String combinerStrategy
    ) {
        TradeExecution tradeExecution = null
        Double upProbability = correlationAssociation.tagProbabilities?.get(combinerStrategy)?.get(upReversalTag.tagName)
        Double downProbability = correlationAssociation.tagProbabilities?.get(combinerStrategy)?.get(downReversalTag.tagName)
        if (upProbability && downProbability) {
            Double modifiedBuyThreshold
            Double modifiedSellThreshold
            if (upProbability > downProbability) {
                Double trendDelta = upProbability - downProbability
                modifiedBuyThreshold = simulation.buyThreshold - (trendDelta * (1 - simulation.buyThreshold) / 2)
                modifiedSellThreshold = simulation.sellThreshold + (trendDelta * (1 - simulation.sellThreshold) / 2)
            } else {
                Double trendDelta = downProbability - upProbability
                modifiedBuyThreshold = simulation.buyThreshold + (trendDelta * (1 - simulation.buyThreshold) / 2)
                modifiedSellThreshold = simulation.sellThreshold - (trendDelta * (1 - simulation.sellThreshold) / 2)
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
