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
class TrendAwareProgressiveThresholdTradeExecutionStrategy implements TradeExecutionStrategy, TagSubset {

    private final String TREND_COMBINER_STRATEGY = 'averageProbabilityCombinerStrategy'

    final static Double MAX_MULTIPLIER = 2
    final static Double MIN_MULTIPLIER = 0.2

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
     * Execute progressive trades while taking trend probabilities into account
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
        Double upProbability = correlationAssociation.tagProbabilities?.get(TREND_COMBINER_STRATEGY)?.get(upTag.tagName)
        Double downProbability = correlationAssociation.tagProbabilities?.get(TREND_COMBINER_STRATEGY)?.get(downTag.tagName)
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
                Double buyThresholdDistance = 1 - modifiedBuyThreshold
                Double actualBuyThresholdDistance = probability - modifiedBuyThreshold
                Double buyMultiplier = MIN_MULTIPLIER + ((actualBuyThresholdDistance / buyThresholdDistance) * (MAX_MULTIPLIER - MIN_MULTIPLIER))
                tradeExecution = new TradeExecution(
                        tradeType: TradeExecution.TradeType.BUY,
                        amount: buyMultiplier * simulation.tradeIncrement,
                        price: correlationAssociation.price
                )
            } else if (tag == sellTag.getTagName() && probability > modifiedSellThreshold) {
                Double sellThresholdDistance = 1 - modifiedSellThreshold
                Double actualSellThresholdDistance = probability - modifiedSellThreshold
                Double sellMultiplier = MIN_MULTIPLIER + ((actualSellThresholdDistance / sellThresholdDistance) * (MAX_MULTIPLIER - MIN_MULTIPLIER))
                tradeExecution = new TradeExecution(
                        tradeType: TradeExecution.TradeType.SELL,
                        amount: sellMultiplier * simulation.tradeIncrement,
                        price: correlationAssociation.price
                )
            }
        }
        return tradeExecution
    }

}
