package com.ukora.tradestudent.strategy.trading.impl

import com.ukora.domain.entities.CorrelationAssociation
import com.ukora.tradestudent.services.simulator.Simulation
import com.ukora.domain.beans.trade.TradeExecution
import com.ukora.tradestudent.strategy.trading.TradeExecutionStrategy
import com.ukora.domain.beans.tags.TagSubset
import com.ukora.domain.beans.tags.buysell.BuySellTagGroup
import com.ukora.domain.beans.tags.buysell.BuyTag
import com.ukora.domain.beans.tags.buysell.SellTag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BalanceAwareThresholdTradeExecutionStategy implements TradeExecutionStrategy, TagSubset {

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
        return "sally"
    }

    /**
     * Execute trades using only thresholds with progressive amounts based on balance
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
        Double amount = (2 * balanceProportion) * simulation.tradeIncrement
        TradeExecution tradeExecution = null
        if (tag == buyTag.getTagName() && probability > simulation.buyThreshold) {
            tradeExecution = new TradeExecution(
                    tradeType: TradeExecution.TradeType.BUY,
                    amount: amount,
                    price: correlationAssociation.price,
                    date: correlationAssociation.date
            )
        } else if (tag == sellTag.getTagName() && probability > simulation.sellThreshold) {
            tradeExecution = new TradeExecution(
                    tradeType: TradeExecution.TradeType.SELL,
                    amount: amount,
                    price: correlationAssociation.price,
                    date: correlationAssociation.date
            )
        }
        return tradeExecution
    }

}
