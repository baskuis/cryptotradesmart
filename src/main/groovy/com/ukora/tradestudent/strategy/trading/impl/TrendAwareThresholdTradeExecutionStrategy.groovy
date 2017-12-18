package com.ukora.tradestudent.strategy.trading.impl

import com.ukora.tradestudent.entities.CorrelationAssociation
import com.ukora.tradestudent.services.simulator.Simulation
import com.ukora.tradestudent.strategy.trading.TradeExecution
import com.ukora.tradestudent.strategy.trading.TradeExecutionStrategy
import com.ukora.tradestudent.tags.TagSubset
import com.ukora.tradestudent.tags.buysell.BuySellTagGroup
import com.ukora.tradestudent.tags.buysell.BuyTag
import com.ukora.tradestudent.tags.buysell.SellTag
import com.ukora.tradestudent.tags.trend.DownTag
import com.ukora.tradestudent.tags.trend.UpTag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TrendAwareThresholdTradeExecutionStrategy implements TradeExecutionStrategy, TagSubset {

    @Autowired
    BuyTag buyTag

    @Autowired
    SellTag sellTag

    @Autowired
    BuySellTagGroup buySellTagGroup

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

    /**
     * Execute trades while taking trend probabilities into account
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
        Double upProbability = correlationAssociation.tagProbabilities?.get(combinerStrategy)?.get(upTag.tagName)
        Double downProbability = correlationAssociation.tagProbabilities?.get(combinerStrategy)?.get(downTag.tagName)
        if(upProbability && downProbability) {
            Double buyDiff = 1 - simulation.buyThreshold
            Double sellDiff = 1 - simulation.sellThreshold
            Double modifiedBuyThreshold = simulation.buyThreshold + (upProbability * buyDiff)
            Double modifiedSellThreshold = simulation.sellThreshold + (downProbability * sellDiff)
            if (tag == buyTag.getTagName() && probability > modifiedBuyThreshold) {
                tradeExecution = new TradeExecution(
                        tradeType: TradeExecution.TradeType.BUY,
                        amount: simulation.tradeIncrement,
                        price: correlationAssociation.price
                )
            } else if (tag == sellTag.getTagName() && probability > modifiedSellThreshold) {
                tradeExecution = new TradeExecution(
                        tradeType: TradeExecution.TradeType.SELL,
                        amount: simulation.tradeIncrement,
                        price: correlationAssociation.price
                )
            }
        }
        return tradeExecution
    }

}
