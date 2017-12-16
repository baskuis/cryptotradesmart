package com.ukora.tradestudent.configuration

import com.ukora.tradestudent.entities.SimulationResult
import com.ukora.tradestudent.services.BytesFetcherService
import com.ukora.tradestudent.strategy.probability.ProbabilityCombinerStrategy
import com.ukora.tradestudent.utils.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Configuration

import javax.annotation.PostConstruct

@Configuration
class ProbabilityCombinerStrategyConfig {

    public static final Double MINIMAL_DIFFERENTIAL = 1.1

    @Autowired
    BytesFetcherService bytesFetcherService

    @Autowired
    ApplicationContext applicationContext

    @PostConstruct
    void disablePoorlyPerformingCombinerStrategies(){
        Map<String, ProbabilityCombinerStrategy> strategyMap = applicationContext.getBeansOfType(ProbabilityCombinerStrategy)
        List<SimulationResult> simulationResults = bytesFetcherService.getSimulations()
        if(simulationResults.size() > 100) {
            strategyMap.each {
                String beanName = it.key
                ProbabilityCombinerStrategy strategy = it.value
                strategy.enabled = simulationResults.findAll {
                    it.probabilityCombinerStrategy == beanName && it.differential > MINIMAL_DIFFERENTIAL
                }.size() > 0
                if(!strategy.enabled) Logger.log("Disabling ${beanName} due to poor performance")
            }
        }
    }

}
