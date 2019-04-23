package com.ukora.tradestudent.services.toolkit

import com.ukora.tradestudent.services.simulator.BuySellTradingHistoricalSimulatorService
import com.ukora.tradestudent.services.simulator.BuySellTradingHistoricalSimulatorService.SimulationSettings
import com.ukora.tradestudent.strategy.probability.ProbabilityCombinerStrategy
import com.ukora.tradestudent.strategy.trading.TradeExecutionStrategy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct

@Component
class ToolkitService {

    Map<String, ProbabilityCombinerStrategy> probabilityCombinerStrategies
    Map<String, TradeExecutionStrategy> tradeExecutionStrategies

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    BuySellTradingHistoricalSimulatorService buySellTradingHistoricalSimulatorService

    @PostConstruct
    def init() {
        probabilityCombinerStrategies = applicationContext.getBeansOfType(ProbabilityCombinerStrategy)
        tradeExecutionStrategies = applicationContext.getBeansOfType(TradeExecutionStrategy)
    }

    void enableProbabilityCombiner(String beanName) {
        probabilityCombinerStrategies.get(beanName)?.enabled = true
    }

    void disableProbabilityCombiner(String beanName) {
        probabilityCombinerStrategies.get(beanName)?.enabled = false
    }

    void enableTradeExecutionStrategy(String beanName) {
        tradeExecutionStrategies.get(beanName)?.enabled = true
    }

    void disableTradeExecutionStrategy(String beanName) {
        tradeExecutionStrategies.get(beanName)?.enabled = false
    }

    SimulationSettings updateSimulationSettings(SimulationSettings simulationSettings) {
        buySellTradingHistoricalSimulatorService.simulationSettings = simulationSettings
        return buySellTradingHistoricalSimulatorService.simulationSettings
    }

    SimulationSettings resetSimulationSettings() {
        buySellTradingHistoricalSimulatorService.resetSimulationSettings()
        return buySellTradingHistoricalSimulatorService.simulationSettings
    }

}
