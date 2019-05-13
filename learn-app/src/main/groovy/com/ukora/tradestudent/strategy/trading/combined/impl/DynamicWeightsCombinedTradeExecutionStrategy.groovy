package com.ukora.tradestudent.strategy.trading.combined.impl

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
import com.ukora.domain.entities.SimulationResult
import com.ukora.domain.entities.TextCorrelationAssociation
import com.ukora.tradestudent.services.simulator.CombinedSimulation
import com.ukora.tradestudent.strategy.trading.combined.CombinedTradeExecutionStrategy
import com.ukora.tradestudent.utils.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DynamicWeightsCombinedTradeExecutionStrategy implements CombinedTradeExecutionStrategy {

    final static String COMBINER_STRATEGY = 'averageTextProbabilityCombinerStrategy'

    @Autowired
    BuySellTagGroup buySellTagGroup
    @Autowired
    UpDownTagGroup upDownTagGroup
    @Autowired
    UpDownReversalTagGroup upDownReversalTagGroup
    @Autowired
    UpDownMovesTagGroup upDownMovesTagGroup

    @Autowired
    BuyTag buyTag
    @Autowired
    UpTag upTag
    @Autowired
    UpReversalTag upReversalTag
    @Autowired
    UpMoveTag upMoveTag

    boolean enabled = true

    @Override
    String getAlias() {
        return 'justice'
    }

    @Override
    boolean isEnabled() {
        return enabled
    }

    @Override
    void setEnabled(boolean enabled) {
        this.enabled = enabled
    }

    static class TagProbabilities {

        BuySellTagGroup buySellTagGroup
        UpDownTagGroup upDownTagGroup
        UpDownReversalTagGroup upDownReversalTagGroup
        UpDownMovesTagGroup upDownMovesTagGroup

        Double buy
        Double up
        Double upReversal
        Double upMove

        Double weight

        SimulationResult simulationResult

        Double getAggregateProbability() {
            Double buyD = buy - 0.5d
            Double upD = up - 0.5d
            Double upReversalD = upReversal - 0.5d
            Double upMoveD = upMove - 0.5d
            Double buyWeight = simulationResult.tagGroupWeights.get(buySellTagGroup.name)
            Double upWeight = simulationResult.tagGroupWeights.get(upDownTagGroup.name)
            Double upReversalWeight = simulationResult.tagGroupWeights.get(upDownReversalTagGroup.name)
            Double upMoveWeight = simulationResult.tagGroupWeights.get(upDownMovesTagGroup.name)
            0.5d + (
                    weight * (
                            (
                                    (buyD * buyWeight) +
                                            (upD * upWeight) +
                                            (upReversalD * upReversalWeight) +
                                            (upMoveD * upMoveWeight)
                            ) / (
                                    Math.abs(buyWeight) +
                                            Math.abs(upWeight) +
                                            Math.abs(upReversalWeight) +
                                            Math.abs(upMoveWeight)
                            )
                    )
            )
        }

        boolean isValid() {
            return (buy && up && upReversal && upMove && weight && simulationResult)
        }
    }

    @Override
    TradeExecution getTrade(
            CorrelationAssociation nca,
            TextCorrelationAssociation tca,
            CombinedSimulation simulation,
            Double balanceProportion
    ) {
        TradeExecution tradeExecution = null

        def numerical = new TagProbabilities(
                buySellTagGroup: buySellTagGroup,
                upDownReversalTagGroup: upDownReversalTagGroup,
                upDownTagGroup: upDownTagGroup,
                upDownMovesTagGroup: upDownMovesTagGroup,
                buy: nca.tagProbabilities.get(simulation.numericalSimulation?.probabilityCombinerStrategy)?.get(buyTag.tagName),
                up: nca.tagProbabilities.get(simulation.numericalSimulation?.probabilityCombinerStrategy)?.get(upTag.tagName),
                upReversal: nca.tagProbabilities.get(simulation.numericalSimulation?.probabilityCombinerStrategy)?.get(upReversalTag.tagName),
                upMove: nca.tagProbabilities.get(simulation.numericalSimulation?.probabilityCombinerStrategy)?.get(upMoveTag.tagName),
                weight: simulation.numericalWeight,
                simulationResult: simulation.numericalSimulation
        )
        def twitter = new TagProbabilities(
                buySellTagGroup: buySellTagGroup,
                upDownReversalTagGroup: upDownReversalTagGroup,
                upDownTagGroup: upDownTagGroup,
                upDownMovesTagGroup: upDownMovesTagGroup,
                buy: tca.strategyProbabilities?.get(COMBINER_STRATEGY)?.TWITTER?.get(buyTag.tagName),
                up: tca.strategyProbabilities?.get(COMBINER_STRATEGY)?.TWITTER?.get(upTag.tagName),
                upReversal: tca.strategyProbabilities?.get(COMBINER_STRATEGY)?.TWITTER?.get(upReversalTag.tagName),
                upMove: tca.strategyProbabilities?.get(COMBINER_STRATEGY)?.TWITTER?.get(upMoveTag.tagName),
                weight: simulation.textTwitterWeight,
                simulationResult: simulation.textTwitterSimulation
        )
        def news = new TagProbabilities(
                buySellTagGroup: buySellTagGroup,
                upDownReversalTagGroup: upDownReversalTagGroup,
                upDownTagGroup: upDownTagGroup,
                upDownMovesTagGroup: upDownMovesTagGroup,
                buy: tca.strategyProbabilities?.get(COMBINER_STRATEGY)?.NEWS?.get(buyTag.tagName),
                up: tca.strategyProbabilities?.get(COMBINER_STRATEGY)?.NEWS?.get(upTag.tagName),
                upReversal: tca.strategyProbabilities?.get(COMBINER_STRATEGY)?.NEWS?.get(upReversalTag.tagName),
                upMove: tca.strategyProbabilities?.get(COMBINER_STRATEGY)?.NEWS?.get(upMoveTag.tagName),
                weight: simulation.textNewsWeight,
                simulationResult: simulation.textNewsSimulation
        )

        if (numerical.valid && twitter.valid && news.valid) {

            Double numericalProbability = numerical.getAggregateProbability()
            Double textNewsProbability = news.getAggregateProbability()
            Double textTwitterProbability = twitter.getAggregateProbability()

            Double aggregateBuyProbability =
                    0.5d + (
                            (
                                    (numericalProbability * numerical.weight) +
                                            (textNewsProbability * news.weight) +
                                            (textTwitterProbability * twitter.weight)
                            ) / (
                                    Math.abs(numerical.weight) +
                                            Math.abs(news.weight) +
                                            Math.abs(twitter.weight)
                            )
                    )

            Double aggregateSellProbability = 1 - aggregateBuyProbability
            if (aggregateBuyProbability > simulation.buyThreshold) {
                tradeExecution = new TradeExecution(
                        tradeType: TradeExecution.TradeType.BUY,
                        amount: simulation.tradeIncrement,
                        price: nca.price,
                        date: nca.date
                )
            } else if (aggregateSellProbability > simulation.sellThreshold) {
                tradeExecution = new TradeExecution(
                        tradeType: TradeExecution.TradeType.SELL,
                        amount: simulation.tradeIncrement,
                        price: nca.price,
                        date: nca.date
                )
            }
        } else {
            if (numerical.valid) {
                Logger.log('Numerical aggregate not valid')
            }
            if (twitter.valid) {
                Logger.log('Twitter aggregate not valid')
            }
            if (news.valid) {
                Logger.log('News aggregate not valid')
            }
        }
        return tradeExecution
    }

}