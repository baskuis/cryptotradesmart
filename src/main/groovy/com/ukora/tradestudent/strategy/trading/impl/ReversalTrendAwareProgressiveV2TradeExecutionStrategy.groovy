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
import com.ukora.tradestudent.tags.trend.DownTag
import com.ukora.tradestudent.tags.trend.UpTag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * This trade strategy combines reversal, trend and short term buy sell associations in it's consideration
 * It's trading strategy is progressive - it will trade according to it's distance from threshold
 *
 * This version uses a different reversal weight - and lower trend division factor
 */
@Component
class ReversalTrendAwareProgressiveV2TradeExecutionStrategy implements TradeExecutionStrategy, TagSubset {

    /**
     * Implementation specific settings
     */
    private final Double TREND_REVERSAL_DIVISION_FACTOR = 1
    private final Double TREND_WEIGHT = 1
    private final Double REVERSAL_WEIGHT = 4

    /**
     * Progressive trading settings
     */
    final static Double MAX_MULTIPLIER = 4
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

    @Autowired
    UpReversalTag upReversalTag

    @Autowired
    DownReversalTag downReversalTag

    @Override
    String getAlias() {
        return "jim"
    }

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

        /**
         * Get reversal probabilities
         */
        Double upReversalProbability = correlationAssociation.tagProbabilities?.get(combinerStrategy)?.get(upReversalTag.tagName)
        Double downReversalProbability = correlationAssociation.tagProbabilities?.get(combinerStrategy)?.get(downReversalTag.tagName)

        /**
         * Get trend probabilities
         */
        Double upProbability = correlationAssociation.tagProbabilities?.get(combinerStrategy)?.get(upTag.tagName)
        Double downProbability = correlationAssociation.tagProbabilities?.get(combinerStrategy)?.get(downTag.tagName)

        if (upReversalProbability && downReversalProbability && upProbability && downProbability) {

            /** Calculate aggregate modifier */
            Double aggregateUpReversalTrendProbability = ((TREND_WEIGHT * upProbability) + (REVERSAL_WEIGHT * upReversalProbability)) / (TREND_WEIGHT + REVERSAL_WEIGHT)
            Double aggregateDownReversalTrendProbability = ((TREND_WEIGHT * downProbability) + (REVERSAL_WEIGHT * downReversalProbability)) / (TREND_WEIGHT + REVERSAL_WEIGHT)

            /** Modified thresholds */
            Double modifiedBuyThreshold
            Double modifiedSellThreshold

            /** Determine if trade execution is relevant */
            if (aggregateUpReversalTrendProbability > aggregateDownReversalTrendProbability) {
                Double trendDelta = aggregateUpReversalTrendProbability - aggregateDownReversalTrendProbability
                modifiedBuyThreshold = simulation.buyThreshold - (trendDelta * (1 - simulation.buyThreshold) / TREND_REVERSAL_DIVISION_FACTOR)
                modifiedSellThreshold = simulation.sellThreshold + (trendDelta * (1 - simulation.sellThreshold) / TREND_REVERSAL_DIVISION_FACTOR)
            } else {
                Double trendDelta = aggregateDownReversalTrendProbability - aggregateUpReversalTrendProbability
                modifiedBuyThreshold = simulation.buyThreshold + (trendDelta * (1 - simulation.buyThreshold) / TREND_REVERSAL_DIVISION_FACTOR)
                modifiedSellThreshold = simulation.sellThreshold - (trendDelta * (1 - simulation.sellThreshold) / TREND_REVERSAL_DIVISION_FACTOR)
            }
            if (tag == buyTag.getTagName() && probability > modifiedBuyThreshold) {
                Double buyThresholdDistance = 1 - modifiedBuyThreshold
                Double actualBuyThresholdDistance = probability - modifiedBuyThreshold
                Double buyMultiplier = MIN_MULTIPLIER + ((actualBuyThresholdDistance / buyThresholdDistance) * (MAX_MULTIPLIER - MIN_MULTIPLIER))
                tradeExecution = new TradeExecution(
                        tradeType: TradeExecution.TradeType.BUY,
                        amount: buyMultiplier * simulation.tradeIncrement,
                        price: correlationAssociation.price,
                        date: correlationAssociation.date
                )
            } else if (tag == sellTag.getTagName() && probability > modifiedSellThreshold) {
                Double sellThresholdDistance = 1 - modifiedSellThreshold
                Double actualSellThresholdDistance = probability - modifiedSellThreshold
                Double sellMultiplier = MIN_MULTIPLIER + ((actualSellThresholdDistance / sellThresholdDistance) * (MAX_MULTIPLIER - MIN_MULTIPLIER))
                tradeExecution = new TradeExecution(
                        tradeType: TradeExecution.TradeType.SELL,
                        amount: sellMultiplier * simulation.tradeIncrement,
                        price: correlationAssociation.price,
                        date: correlationAssociation.date
                )
            }

        }
        return tradeExecution
    }

}
