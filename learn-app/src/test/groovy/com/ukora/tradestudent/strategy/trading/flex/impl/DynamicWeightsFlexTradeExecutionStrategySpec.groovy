package com.ukora.tradestudent.strategy.trading.flex.impl

import com.ukora.domain.beans.tags.buysell.BuySellTagGroup
import com.ukora.domain.beans.tags.buysell.BuyTag
import com.ukora.domain.beans.tags.moves.UpDownMovesTagGroup
import com.ukora.domain.beans.tags.moves.UpMoveTag
import com.ukora.domain.beans.tags.reversal.UpDownReversalTagGroup
import com.ukora.domain.beans.tags.reversal.UpReversalTag
import com.ukora.domain.beans.tags.trend.UpDownTagGroup
import com.ukora.domain.beans.tags.trend.UpTag
import com.ukora.domain.entities.CorrelationAssociation
import com.ukora.domain.entities.TextCorrelationAssociation
import com.ukora.tradestudent.services.simulator.Simulation
import spock.lang.Specification
import spock.lang.Unroll

class DynamicWeightsFlexTradeExecutionStrategySpec extends Specification {

    DynamicWeightsFlexTradeExecutionStrategy dynamicWeightsFlexTradeExecutionStrategy = new DynamicWeightsFlexTradeExecutionStrategy()

    BuySellTagGroup buySellTagGroup = new BuySellTagGroup()
    UpDownTagGroup upDownTagGroup = new UpDownTagGroup()
    UpDownReversalTagGroup upDownReversalTagGroup = new UpDownReversalTagGroup()
    UpDownMovesTagGroup upDownMovesTagGroup = new UpDownMovesTagGroup()

    BuyTag buyTag = new BuyTag()
    UpTag upTag = new UpTag()
    UpReversalTag upReversalTag = new UpReversalTag()
    UpMoveTag upMoveTag = new UpMoveTag()

    def setup() {
        dynamicWeightsFlexTradeExecutionStrategy.buyTag = buyTag
        dynamicWeightsFlexTradeExecutionStrategy.upTag = upTag
        dynamicWeightsFlexTradeExecutionStrategy.upReversalTag = upReversalTag
        dynamicWeightsFlexTradeExecutionStrategy.upMoveTag = upMoveTag
        dynamicWeightsFlexTradeExecutionStrategy.buySellTagGroup = buySellTagGroup
        dynamicWeightsFlexTradeExecutionStrategy.upDownTagGroup = upDownTagGroup
        dynamicWeightsFlexTradeExecutionStrategy.upDownReversalTagGroup = upDownReversalTagGroup
        dynamicWeightsFlexTradeExecutionStrategy.upDownMovesTagGroup = upDownMovesTagGroup
    }

    @Unroll
    def "test #scenario"() {

        setup:
        TextCorrelationAssociation textCorrelationAssociation = new TextCorrelationAssociation()
        CorrelationAssociation correlationAssociation = new CorrelationAssociation(
                tagProbabilities: [
                        'combiner1': [
                                (upTag.tagName)        : up,
                                (upReversalTag.tagName): upreveral,
                                (buyTag.tagName)       : buy,
                                (upMoveTag.tagName)    : upmove
                        ]
                ]
        )
        Simulation simulation = new Simulation(
                buyThreshold: buyThreshold,
                sellThreshold: 0.55,
                tradeIncrement: 0.2,
                tagGroupWeights: [
                        (upDownTagGroup.name)        : upWeight,
                        (upDownReversalTagGroup.name): upreveralWeight,
                        (buySellTagGroup.name)       : buyWeight,
                        (upDownMovesTagGroup.name)   : upmoveWeight
                ]
        )
        String combinerStrategy = 'combiner1'
        Double balanceProportion = 0.5d

        when:
        def r = dynamicWeightsFlexTradeExecutionStrategy.getTrade(correlationAssociation, textCorrelationAssociation, simulation, combinerStrategy, balanceProportion)

        then:
        if (expected) {
            assert r.amount == expected
        } else {
            assert !r
        }
        noExceptionThrown()

        where:
        scenario                                         | expected | up    | upreveral | buy   | upmove | buyThreshold | upWeight | upreveralWeight | buyWeight | upmoveWeight
        'all positive weights does not affect p value'   | 0.2      | 0.55d | 0.55d     | 0.55d | 0.55d  | 0.54         | 1        | 1               | 1         | 1
        'large negative weight reduces computed p value' | null     | 0.55d | 0.55d     | 0.55d | 0.55d  | 0.54         | 1        | -1              | 1         | 1
        'small negative weight reduces computed p value' | 0.2      | 0.55d | 0.55d     | 0.55d | 0.55d  | 0.53         | 1        | -0.1d           | 1         | 1

    }

}
