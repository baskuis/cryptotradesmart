package com.ukora.tradestudent.strategy.trading

import com.ukora.tradestudent.entities.CorrelationAssociation
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

    TradeExecution getTrade(
            CorrelationAssociation correlationAssociation,
            String tag,
            Double probability,
            Simulation simulation,
            String combinerStrategy
    )

}