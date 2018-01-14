package com.ukora.tradestudent.strategy.trading.impl

import com.ukora.tradestudent.entities.CorrelationAssociation
import com.ukora.tradestudent.services.simulator.Simulation
import com.ukora.tradestudent.strategy.trading.TradeExecution
import com.ukora.tradestudent.strategy.trading.TradeExecutionStrategy
import com.ukora.tradestudent.tags.buysell.BuySellTagGroup
import com.ukora.tradestudent.tags.buysell.BuyTag
import com.ukora.tradestudent.tags.buysell.SellTag
import com.ukora.tradestudent.tags.TagSubset
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ProgressiveThresholdTradeExecutionStrategy implements TradeExecutionStrategy, TagSubset {

    @Autowired
    BuyTag buyTag

    @Autowired
    SellTag sellTag

    @Autowired
    BuySellTagGroup buySellTagGroup

    @Override
    boolean applies(String toTag) {
        return buySellTagGroup.applies(toTag)
    }

    final static Double MAX_MULTIPLIER = 2
    final static Double MIN_MULTIPLIER = 0.2

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
     * Multiplier on amount depending on distance from configured threshold
     * configured with min/max value. This value modifies the amount which is traded
     *
     * @param correlationAssociation
     * @param tag
     * @param probability
     * @param simulation
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
        if (tag == buyTag.getTagName() && probability > simulation.buyThreshold) {
            Double buyThresholdDistance = 1 - simulation.buyThreshold
            Double actualBuyThresholdDistance = probability - simulation.buyThreshold
            Double buyMultiplier = MIN_MULTIPLIER + ((actualBuyThresholdDistance / buyThresholdDistance) * (MAX_MULTIPLIER - MIN_MULTIPLIER))
            tradeExecution = new TradeExecution(
                    tradeType: TradeExecution.TradeType.BUY,
                    amount: buyMultiplier * simulation.tradeIncrement,
                    price: correlationAssociation.price,
                    date: correlationAssociation.date
            )
        } else if (tag == sellTag.getTagName() && probability > simulation.sellThreshold) {
            Double sellThresholdDistance = 1 - simulation.sellThreshold
            Double actualSellThresholdDistance = probability - simulation.sellThreshold
            Double sellMultiplier = MIN_MULTIPLIER + ((actualSellThresholdDistance / sellThresholdDistance) * (MAX_MULTIPLIER - MIN_MULTIPLIER))
            tradeExecution = new TradeExecution(
                    tradeType: TradeExecution.TradeType.SELL,
                    amount: sellMultiplier * simulation.tradeIncrement,
                    price: correlationAssociation.price,
                    date: correlationAssociation.date
            )
        }
        return tradeExecution
    }

}
