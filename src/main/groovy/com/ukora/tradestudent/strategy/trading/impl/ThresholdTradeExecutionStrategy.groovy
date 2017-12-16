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
class ThresholdTradeExecutionStrategy implements TradeExecutionStrategy, TagSubset {

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
    TradeExecution getTrade(
            CorrelationAssociation correlationAssociation,
            String tag,
            Double probability,
            Simulation simulation,
            String combinerStrategy
    ) {
        TradeExecution tradeExecution = null
        if (tag == buyTag.getTagName() && probability > simulation.buyThreshold) {
            tradeExecution = new TradeExecution(
                    tradeType: TradeExecution.TradeType.BUY,
                    amount: simulation.tradeIncrement,
                    price: correlationAssociation.price
            )
        } else if (tag == sellTag.getTagName() && probability > simulation.sellThreshold) {
            tradeExecution = new TradeExecution(
                    tradeType: TradeExecution.TradeType.SELL,
                    amount: simulation.tradeIncrement,
                    price: correlationAssociation.price
            )
        }
        return tradeExecution
    }

}
