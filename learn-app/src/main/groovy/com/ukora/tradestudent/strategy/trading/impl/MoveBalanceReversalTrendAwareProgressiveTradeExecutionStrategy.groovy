package com.ukora.tradestudent.strategy.trading.impl

import com.ukora.domain.beans.tags.TagSubset
import com.ukora.domain.beans.tags.buysell.BuySellTagGroup
import com.ukora.domain.beans.tags.buysell.BuyTag
import com.ukora.domain.beans.tags.buysell.SellTag
import com.ukora.domain.beans.tags.moves.DownMoveTag
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
 * This trade strategy combines market moves, reversals, trend and short term buy sell associations in it's consideration
 * It's trading strategy is progressive - it will trade according to it's distance from threshold
 * It's also balance aware and will trade in amounts according to it's available balance
 *
 */
@Component
class MoveBalanceReversalTrendAwareProgressiveTradeExecutionStrategy implements TradeExecutionStrategy, TagSubset {

    /**
     * Implementation specific settings
     */
    private final Double TREND_MOVE_REVERSAL_DIVISION_FACTOR = 2
    private final Double TREND_WEIGHT = 1
    private final Double MOVE_WEIGHT = 1
    private final Double REVERSAL_WEIGHT = 2

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

    @Autowired
    UpMoveTag upMoveTag

    @Autowired
    DownMoveTag downMoveTag

    @Override
    String getAlias() {
        return "emmy"
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
         * The amount is progressive based on balance proportion
         */
        Double amount = (2 * balanceProportion) * simulation.tradeIncrement

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

        /**
         * Get move probabilities
         */
        Double upMoveProbability = correlationAssociation.tagProbabilities?.get(combinerStrategy)?.get(upMoveTag.tagName)
        Double downMoveProbability = correlationAssociation.tagProbabilities?.get(combinerStrategy)?.get(downMoveTag.tagName)

        if (upReversalProbability && downReversalProbability && upProbability && downProbability && upMoveProbability && downMoveProbability) {

            /** Calculate aggregate modifier */
            Double aggregateMoveUpReversalTrendProbability = ((TREND_WEIGHT * upProbability) + (REVERSAL_WEIGHT * upReversalProbability) + (MOVE_WEIGHT * upMoveProbability)) / (TREND_WEIGHT + REVERSAL_WEIGHT + MOVE_WEIGHT)
            Double aggregateMoveDownReversalTrendProbability = ((TREND_WEIGHT * downProbability) + (REVERSAL_WEIGHT * downReversalProbability) + (MOVE_WEIGHT * downMoveProbability)) / (TREND_WEIGHT + REVERSAL_WEIGHT + MOVE_WEIGHT)

            /** Modified thresholds */
            Double modifiedBuyThreshold
            Double modifiedSellThreshold

            /** Determine if trade execution is relevant */
            if (aggregateMoveUpReversalTrendProbability > aggregateMoveDownReversalTrendProbability) {
                Double trendDelta = aggregateMoveUpReversalTrendProbability - aggregateMoveDownReversalTrendProbability
                modifiedBuyThreshold = simulation.buyThreshold - (trendDelta * (1 - simulation.buyThreshold) / TREND_MOVE_REVERSAL_DIVISION_FACTOR)
                modifiedSellThreshold = simulation.sellThreshold + (trendDelta * (1 - simulation.sellThreshold) / TREND_MOVE_REVERSAL_DIVISION_FACTOR)
            } else {
                Double trendDelta = aggregateMoveDownReversalTrendProbability - aggregateMoveUpReversalTrendProbability
                modifiedBuyThreshold = simulation.buyThreshold + (trendDelta * (1 - simulation.buyThreshold) / TREND_MOVE_REVERSAL_DIVISION_FACTOR)
                modifiedSellThreshold = simulation.sellThreshold - (trendDelta * (1 - simulation.sellThreshold) / TREND_MOVE_REVERSAL_DIVISION_FACTOR)
            }
            if (tag == buyTag.getTagName() && probability > modifiedBuyThreshold) {
                Double buyThresholdDistance = 1 - modifiedBuyThreshold
                Double actualBuyThresholdDistance = probability - modifiedBuyThreshold
                Double buyMultiplier = MIN_MULTIPLIER + ((actualBuyThresholdDistance / buyThresholdDistance) * (MAX_MULTIPLIER - MIN_MULTIPLIER))
                tradeExecution = new TradeExecution(
                        tradeType: TradeExecution.TradeType.BUY,
                        amount: buyMultiplier * amount,
                        price: correlationAssociation.price,
                        date: correlationAssociation.date
                )
            } else if (tag == sellTag.getTagName() && probability > modifiedSellThreshold) {
                Double sellThresholdDistance = 1 - modifiedSellThreshold
                Double actualSellThresholdDistance = probability - modifiedSellThreshold
                Double sellMultiplier = MIN_MULTIPLIER + ((actualSellThresholdDistance / sellThresholdDistance) * (MAX_MULTIPLIER - MIN_MULTIPLIER))
                tradeExecution = new TradeExecution(
                        tradeType: TradeExecution.TradeType.SELL,
                        amount: sellMultiplier * amount,
                        price: correlationAssociation.price,
                        date: correlationAssociation.date
                )
            }

        }
        return tradeExecution
    }

}
