package com.ukora.tradestudent.strategy.trading

import com.ukora.domain.beans.trade.TradeExecution
import com.ukora.domain.entities.CorrelationAssociation
import com.ukora.tradestudent.services.simulator.Simulation

/**
 * This contract defines a trade execution strategy
 * Here implementations can be tested for performance
 * A simple implementation would trade using thresholds and always
 * the same increment. Other strategies could be more effective
 *
 *
 */
interface TradeExecutionStrategy {

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
     * @param tag The name of the correlation tag
     * @param probability The tag probability
     * @param simulation The winning simulation configuration
     * @param combinerStrategy The bean name of the combiner strategy
     * @param balanceProportion The proportion of balance A vs balance A + B priced in A
     * @return
     */
    TradeExecution getTrade(
            CorrelationAssociation correlationAssociation,
            String tag,
            Double probability,
            Simulation simulation,
            String combinerStrategy,
            Double balanceProportion
    )

}