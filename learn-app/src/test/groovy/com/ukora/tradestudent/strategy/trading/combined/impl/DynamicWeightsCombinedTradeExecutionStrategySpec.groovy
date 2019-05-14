package com.ukora.tradestudent.strategy.trading.combined.impl

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
import com.ukora.tradestudent.services.simulator.CombinedSimulation
import spock.lang.Ignore
import spock.lang.Specification

@Ignore //TODO: Finish spec
class DynamicWeightsCombinedTradeExecutionStrategySpec extends Specification {

    DynamicWeightsCombinedTradeExecutionStrategy dynamicWeightsCombinedTradeExecutionStrategy = new DynamicWeightsCombinedTradeExecutionStrategy()

    BuySellTagGroup buySellTagGroup = new BuySellTagGroup()
    UpDownTagGroup upDownTagGroup = new UpDownTagGroup()
    UpDownReversalTagGroup upDownReversalTagGroup = new UpDownReversalTagGroup()
    UpDownMovesTagGroup upDownMovesTagGroup = new UpDownMovesTagGroup()
    BuyTag buyTag = new BuyTag()
    UpTag upTag = new UpTag()
    UpReversalTag upReversalTag = new UpReversalTag()
    UpMoveTag upMoveTag = new UpMoveTag()

    def setup() {
        dynamicWeightsCombinedTradeExecutionStrategy.buySellTagGroup = buySellTagGroup
        dynamicWeightsCombinedTradeExecutionStrategy.upDownTagGroup = upDownTagGroup
        dynamicWeightsCombinedTradeExecutionStrategy.upDownReversalTagGroup = upDownReversalTagGroup
        dynamicWeightsCombinedTradeExecutionStrategy.upDownMovesTagGroup = upDownMovesTagGroup
        dynamicWeightsCombinedTradeExecutionStrategy.buyTag = buyTag
        dynamicWeightsCombinedTradeExecutionStrategy.upTag = upTag
        dynamicWeightsCombinedTradeExecutionStrategy.upReversalTag = upReversalTag
        dynamicWeightsCombinedTradeExecutionStrategy.upMoveTag = upMoveTag
    }

    def "test trade #scenario"() {

        setup:
        CorrelationAssociation nca = new CorrelationAssociation()
        TextCorrelationAssociation tca = new TextCorrelationAssociation()
        CombinedSimulation simulation = new CombinedSimulation()

        when:
        def r = dynamicWeightsCombinedTradeExecutionStrategy.getTrade(
                nca, tca, simulation, proportion
        )

        then:
        noExceptionThrown()
        0 * _

        where:
        scenario          | proportion
        "half proportion" | 0.5

    }

}
