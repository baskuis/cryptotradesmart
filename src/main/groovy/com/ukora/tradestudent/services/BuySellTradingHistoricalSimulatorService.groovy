package com.ukora.tradestudent.services

import com.ukora.tradestudent.entities.CorrelationAssociation
import com.ukora.tradestudent.strategy.ProbabilityCombinerStrategy
import com.ukora.tradestudent.utils.Logger
import groovy.util.logging.Log4j2
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import java.time.Duration
import java.time.Instant

/**
 * TODO: Run simulations with all thresholds at the same time
 * TODO: Run simulations with multiple values for trade increments
 * TODO: Sort simulations by most profitable - strategy and configuration
 * TODO: Create concept of trading strategies
 *
 *
 */
@Log4j2
@Service
class BuySellTradingHistoricalSimulatorService {

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

    private final static Double STARTING_BALANCE = 10
    private final static Double TRADE_INCREMENT = 0.5
    private final static Double TRADE_TRANSACTION_COST = 0.0025
    private final static Double BUY_THRESHOLD = 0.80
    private final static Double SELL_THRESHOLD = 0.90

    @Async
    Map runSimulation(Date fromDate){
        Map<String, Double> balancesA = [:]
        Map<String, Double> balancesB = [:]
        if(!fromDate) return null
        Instant end = Instant.now()
        Instant start = Instant.ofEpochMilli(fromDate.time)
        Duration gap = Duration.ofSeconds(INTERVAL_SECONDS);
        Instant current = start
        Double finalPrice
        while (current.isBefore(end)) {
            current = current + gap
            CorrelationAssociation correlationAssociation = probabilityFigurerService.getCorrelationAssociations(Date.from(current))
            correlationAssociation.tagProbabilities.each {
                String strategy = it.key
                it.value.each {
                    if(!correlationAssociation.price) return
                    if(!it.value) return
                    finalPrice = correlationAssociation.price
                    String tag = it.key
                    Double probability = it.value
                    Double balanceB = balancesB.get(strategy, 0)
                    Double balanceA = balancesA.get(strategy, STARTING_BALANCE)
                    Double costsA = TRADE_INCREMENT * correlationAssociation.price * (1 + TRADE_TRANSACTION_COST)
                    Double proceedsA = TRADE_INCREMENT * correlationAssociation.price * (1 - TRADE_TRANSACTION_COST)
                    if(tag == 'buy' && probability > BUY_THRESHOLD){
                        Logger.log("Buying A for B, p: " + probability)
                        if(balanceB < costsA){
                            Logger.log("Not enough balanceB left: " + balanceB)
                        }else{
                            balancesA.put(strategy, balanceA + (TRADE_INCREMENT * (1 + TRADE_TRANSACTION_COST)))
                            balancesB.put(strategy, balanceB - (TRADE_INCREMENT * correlationAssociation.price))
                        }
                    }
                    if(tag == 'sell' && probability > SELL_THRESHOLD){
                        Logger.log("Selling A for B, p: " + probability)
                        if(balanceA < TRADE_INCREMENT * (1 + TRADE_TRANSACTION_COST)){
                            Logger.log("Not enough balanceA left: " + balanceA)
                        }else{
                            balancesA.put(strategy, balanceA - TRADE_INCREMENT)
                            balancesB.put(strategy, balanceB + proceedsA)
                        }
                    }
                }
            }
        }
        Map result = [
                'A': balancesA,
                'B': balancesB,
                'Result': [:]
        ]
        result.A.each {
            result.Result.put(it.key, it.value + (result.B.get(it.key) / finalPrice))
        }
        Logger.log(result as String)
        return result
    }

}
