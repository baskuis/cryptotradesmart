package com.ukora.tradestudent.strategy.trading.impl

import com.ukora.domain.beans.tags.TagSubset
import com.ukora.domain.beans.tags.buysell.BuySellTagGroup
import com.ukora.domain.beans.tags.buysell.BuyTag
import com.ukora.domain.beans.tags.buysell.SellTag
import com.ukora.domain.beans.tags.reversal.DownReversalTag
import com.ukora.domain.beans.tags.reversal.UpReversalTag
import com.ukora.domain.beans.tags.trend.DownTag
import com.ukora.domain.beans.tags.trend.UpTag
import com.ukora.domain.beans.trade.TradeExecution
import com.ukora.domain.entities.CorrelationAssociation
import com.ukora.domain.entities.ExtractedText
import com.ukora.tradestudent.services.simulator.Simulation
import com.ukora.tradestudent.services.text.ConcurrentTextAssociationProbabilityService
import com.ukora.tradestudent.strategy.trading.TradeExecutionStrategy
import com.ukora.tradestudent.utils.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TwitterBalanceReversalTrendAwareProgressiveTradeExecutionStrategy implements TradeExecutionStrategy, TagSubset {

    /**
     * Implementation specific settings
     */
    private final Double TREND_REVERSAL_DIVISION_FACTOR = 2
    private final Double TREND_WEIGHT = 1
    private final Double REVERSAL_WEIGHT = 2
    private final Double TEXT_BUYSELL_WEIGHT = 0.2
    private final Double TEXT_UPDOWN_WEIGHT = 0.2
    private final Double TEXT_REVERSAL_WEIGHT = 0.1

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
    ConcurrentTextAssociationProbabilityService concurrentTextAssociationProbabilityService

    @Override
    String getAlias() {
        return "terry"
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

        String textSource = ExtractedText.TextSource.TWITTER.name()
        Double buyTextProbability
        Double sellTextProbability
        Double upTextProbability
        Double downTextProbability
        Double upReversalTextProbability
        Double downReversalTextProbability
        try {
            String textCStrategy = 'averageTextProbabilityCombinerStrategy'
            Map textAssociations = concurrentTextAssociationProbabilityService.tagCorrelationByText(correlationAssociation.date)
            buyTextProbability = textAssociations?.get(textCStrategy)?.get(textSource)?.get(buyTag.tagName)
            sellTextProbability = textAssociations?.get(textCStrategy)?.get(textSource)?.get(sellTag.tagName)
            upTextProbability = textAssociations?.get(textCStrategy)?.get(textSource)?.get(upTag.tagName)
            downTextProbability = textAssociations?.get(textCStrategy)?.get(textSource)?.get(downTag.tagName)
            upReversalTextProbability = textAssociations?.get(textCStrategy)?.get(textSource)?.get(upReversalTag.tagName)
            downReversalTextProbability = textAssociations?.get(textCStrategy)?.get(textSource)?.get(downReversalTag.tagName)
        } catch (Exception e) {
            Logger.log(
                    'Unable to extract text p values for ' + textSource +
                            ' message:' + e.message +
                            ' in strategy: ' + TwitterBalanceReversalTrendAwareProgressiveTradeExecutionStrategy.name
            )
            return
        }
        if (
        !buyTextProbability ||
                !sellTextProbability ||
                !upTextProbability ||
                !downTextProbability ||
                !upReversalTextProbability ||
                !downReversalTextProbability
        ) {
            Logger.log('Missing text p value' +
                    'buyTextProbability: [' + buyTextProbability + '] ' +
                    'sellTextProbability: [' + buyTextProbability + '] ' +
                    'upTextProbability: [' + buyTextProbability + '] ' +
                    'downTextProbability: [' + downTextProbability + '] ' +
                    'upReversalTextProbability: [' + upReversalTextProbability + '] ' +
                    'downReversalTextProbability: [' + downReversalTextProbability + '] '
            )
            return
        }

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

        if (upReversalProbability && downReversalProbability && upProbability && downProbability) {

            /** Calculate aggregate modifier */
            Double aggregateUpReversalTrendProbability = ((TREND_WEIGHT * upProbability) + (REVERSAL_WEIGHT * upReversalProbability) + (TEXT_UPDOWN_WEIGHT * upTextProbability) + (TEXT_BUYSELL_WEIGHT * buyTextProbability) + (TEXT_REVERSAL_WEIGHT * upReversalTextProbability)) / (TREND_WEIGHT + REVERSAL_WEIGHT + TEXT_UPDOWN_WEIGHT + TEXT_BUYSELL_WEIGHT + TEXT_REVERSAL_WEIGHT)
            Double aggregateDownReversalTrendProbability = ((TREND_WEIGHT * downProbability) + (REVERSAL_WEIGHT * downReversalProbability) + (TEXT_UPDOWN_WEIGHT * downTextProbability) + (TEXT_BUYSELL_WEIGHT * downTextProbability) + (TEXT_REVERSAL_WEIGHT * downReversalTextProbability)) / (TREND_WEIGHT + REVERSAL_WEIGHT + TEXT_UPDOWN_WEIGHT + TEXT_BUYSELL_WEIGHT + TEXT_REVERSAL_WEIGHT)

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
