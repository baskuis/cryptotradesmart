package com.ukora.tradestudent.services

import com.ukora.tradestudent.entities.CorrelationAssociation
import com.ukora.tradestudent.strategy.ProbabilityCombinerStrategy
import groovy.util.logging.Log4j2
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import java.time.Duration
import java.time.Instant

@Log4j2
@Service
class TradingHistoricalSimulatorService {

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    ProbabilityFigurerService probabilityFigurerService

    Map<String, ProbabilityCombinerStrategy> probabilityCombinerStrategyMap

    public final static long INTERVAL_SECONDS = 60

    @PostConstruct
    void init(){
        probabilityCombinerStrategyMap = applicationContext.getBeansOfType(ProbabilityCombinerStrategy)
    }

    Map runSimulation(Date fromDate){
        if(!fromDate) return null
        Instant end = Instant.now()
        Instant start = Instant.ofEpochMilli(fromDate.time)
        Duration gap = Duration.ofSeconds(INTERVAL_SECONDS);
        Instant current = start
        while (current.isBefore(end)) {
            current = current + gap
            CorrelationAssociation correlationAssociation = probabilityFigurerService.getCorrelationAssociations(Date.from(current))
            println correlationAssociation
        }
        return [:]
    }

}
