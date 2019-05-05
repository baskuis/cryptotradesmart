package com.ukora.tradestudent.strategy.trading.combined

import com.ukora.domain.beans.trade.TradeExecution
import com.ukora.domain.entities.CorrelationAssociation
import com.ukora.domain.entities.TextCorrelationAssociation
import com.ukora.tradestudent.services.simulator.Simulation

interface CombinedTradeExecutionStrategy {

    /**
     * Indicates alias for this algorithm
     *
     * @return
     */
    String getAlias()

    boolean isEnabled()

    void setEnabled(boolean enabled)

    /**
     * This method will product a buy/sell trade order based on the following
     * inputs. Each strategy which implements this interface can handle
     * these values differently. In the buy/sell trading historical simulator service
     * all combinations of implementations of this interface will be tested
     *
     * @param correlationAssociation The correlation association object
     * @param textCorrelationAssociation The text correlation association object
     * @param simulationA The winning simulation configuration
     * @param simulationB The winning simulation configuration
     * @param combinerStrategy The bean name of the combiner strategy
     * @param balanceProportion The proportion of balance A vs balance A + B priced in A
     * @return
     */
    TradeExecution getTrade(
            CorrelationAssociation correlationAssociation,
            TextCorrelationAssociation textCorrelationAssociation,
            Simulation simulationA,
            Simulation simulationB,
            String combinerStrategy,
            Double balanceProportion
    )

}

