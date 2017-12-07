package com.ukora.tradestudent.services

import com.ukora.tradestudent.entities.CorrelationAssociation
import com.ukora.tradestudent.strategy.probability.ProbabilityCombinerStrategy
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

    Map<String, Map<String, Object>> simulations = [:]

    private final static Double STARTING_BALANCE = 10
    private final static Double TRADE_INCREMENT = 0.5
    private final static Double TRADE_TRANSACTION_COST = 0.0020
    private final static Double LOWEST_THRESHOLD = 0.60
    private final static Double HIGHEST_THRESHOLD = 1.00

    @PostConstruct
    void init(){

        /**
         * Get probability combiner strategies
         */
        probabilityCombinerStrategyMap = applicationContext.getBeansOfType(ProbabilityCombinerStrategy)

        /**
         * Build simulations
         */
        for(Double tradeIncrement = TRADE_INCREMENT; tradeIncrement < STARTING_BALANCE; tradeIncrement += TRADE_INCREMENT) {
            for (Double thresholdBuy = LOWEST_THRESHOLD; thresholdBuy <= HIGHEST_THRESHOLD; thresholdBuy += 0.01) {
                for (Double thresholdSell = LOWEST_THRESHOLD; thresholdSell <= HIGHEST_THRESHOLD; thresholdSell += 0.01) {
                    simulations.put(String.format("buy:%s,sell:%sinc:%s", thresholdBuy, thresholdSell, tradeIncrement), [
                            'buyThreshold'   : thresholdBuy,
                            'sellThreshold'  : thresholdSell,
                            'startingBalance': STARTING_BALANCE,
                            'tradeIncrement' : tradeIncrement,
                            'transactionCost': TRADE_TRANSACTION_COST,
                            'tradeCount'     : 0,
                            'balancesA'      : [:],
                            'balancesB'      : [:]
                    ])
                }
            }
        }

    }

    @Async
    Map runSimulation(Date fromDate){
        if(!fromDate) return null
        Instant end = Instant.now()
        Instant start = Instant.ofEpochMilli(fromDate.time)
        Duration gap = Duration.ofSeconds(INTERVAL_SECONDS)
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
                    simulations.each {
                        Map simulation = it.value
                        Double costsA = (simulation.get('tradeIncrement') as Double) * correlationAssociation.price * (1 + (simulation.get('transactionCost') as Double))
                        Double proceedsA = (simulation.get('tradeIncrement') as Double) * correlationAssociation.price * (1 - (simulation.get('transactionCost') as Double))
                        Double balanceB = (simulation.get('balancesB') as Map).get(strategy, 0) as Double
                        Double balanceA = (simulation.get('balancesA') as Map).get(strategy, STARTING_BALANCE) as Double
                        if(tag == 'buy' && probability > (simulation.get('buyThreshold') as Double)){
                            Logger.debug("Buying A for B, p: " + probability)
                            if(balanceB < costsA){
                                Logger.debug("Not enough balanceB left: " + balanceB)
                            }else{
                                (simulation.get('balancesA') as Map).put(strategy, balanceA + ((simulation.get('tradeIncrement') as Double) * (1 + (simulation.get('transactionCost') as Double))))
                                (simulation.get('balancesB') as Map).put(strategy, balanceB - ((simulation.get('tradeIncrement') as Double) * correlationAssociation.price))
                                simulation.put('tradeCount', (simulation.get('tradeCount', 0) as Integer) + 1)
                            }
                        }
                        if(tag == 'sell' && probability > (simulation.get('sellThreshold') as Double)){
                            Logger.debug("Selling A for B, p: " + probability)
                            if(balanceA < (simulation.get('tradeIncrement') as Double) * (1 + (simulation.get('transactionCost') as Double))){
                                Logger.debug("Not enough balanceA left: " + balanceA)
                            }else{
                                (simulation.get('balancesA') as Map).put(strategy, balanceA - (simulation.get('tradeIncrement') as Double))
                                (simulation.get('balancesB') as Map).put(strategy, balanceB + proceedsA)
                                simulation.put('tradeCount', (simulation.get('tradeCount', 0) as Integer) + 1)
                            }
                        }
                    }
                }
            }
        }

        Map<String, Map> result = [:]
        simulations.each {
            String simulationKey = it.key
            Map simulation = it.value
            (simulation.get('balancesA') as Map).each {
                String strategy = it.key
                Double finalBalance = it.value + ((simulation.get('balancesA') as Map).get(it.key) / finalPrice)
                (simulation.get('result', [:]) as Map).put(it.key, finalBalance)
                result.put(String.format('%s:%s', simulationKey, strategy), [
                        'balance' : finalBalance,
                        'tradeCount' : simulation.get('tradeCount')
                ])
            }
        }

        Logger.log('results are in')
        Logger.log(result.sort { -it.value.get('balance') }.take(20) as String)

        return result

    }

}
