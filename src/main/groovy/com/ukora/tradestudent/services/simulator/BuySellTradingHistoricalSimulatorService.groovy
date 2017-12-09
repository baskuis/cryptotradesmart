package com.ukora.tradestudent.services.simulator

import com.ukora.tradestudent.entities.CorrelationAssociation
import com.ukora.tradestudent.services.ProbabilityFigurerService
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

    List<Simulation> simulations = []

    private final static Double STARTING_BALANCE = 10
    private final static Double TRADE_INCREMENT = 1
    private final static Double TRADE_TRANSACTION_COST = 0.0020
    private final static Double LOWEST_THRESHOLD = 0.50
    private final static Double HIGHEST_THRESHOLD = 1.00
    private final static Double THRESHOLD_INCREMENT = 0.02

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
            for (Double thresholdBuy = LOWEST_THRESHOLD; thresholdBuy <= HIGHEST_THRESHOLD; thresholdBuy += THRESHOLD_INCREMENT) {
                for (Double thresholdSell = LOWEST_THRESHOLD; thresholdSell <= HIGHEST_THRESHOLD; thresholdSell += THRESHOLD_INCREMENT) {
                    simulations.add(
                        new Simulation(
                            key            : String.format("buy:%s,sell:%s,inc:%s", thresholdBuy, thresholdSell, tradeIncrement),
                            buyThreshold   : thresholdBuy,
                            sellThreshold  : thresholdSell,
                            startingBalance: STARTING_BALANCE,
                            tradeIncrement : tradeIncrement,
                            transactionCost: TRADE_TRANSACTION_COST,
                            tradeCount     : 0,
                            balancesA      : [:],
                            balancesB      : [:]
                        )
                    )
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
            int numCores = Runtime.getRuntime().availableProcessors()
            correlationAssociation.tagProbabilities.each {
                String strategy = it.key
                it.value.each {
                    if(!correlationAssociation.price) return
                    if(!it.value) return
                    finalPrice = correlationAssociation.price
                    String tag = it.key
                    Double probability = it.value
                    def partitioned = (0..<numCores).collect { i ->
                        simulations[ (i..<simulations.size()).step( numCores ) ]
                    }
                    def threads = partitioned.collect { group ->
                        Thread.start({
                            group.each {
                                Simulation simulation = it
                                Double costsA = simulation.tradeIncrement * correlationAssociation.price * (1 + (simulation.transactionCost as Double))
                                Double proceedsA = (simulation.tradeIncrement as Double) * correlationAssociation.price * (1 - (simulation.transactionCost as Double))
                                Double balanceB = (simulation.balancesB as Map).get(strategy, 0) as Double
                                Double balanceA = (simulation.balancesA as Map).get(strategy, STARTING_BALANCE) as Double
                                switch (tag) {
                                    case 'buy':
                                        if (probability > (simulation.buyThreshold as Double)) {
                                            Logger.debug("Buying A for B, p: " + probability)
                                            if (balanceB < costsA) {
                                                Logger.debug("Not enough balanceB left: " + balanceB)
                                            } else {
                                                (simulation.balancesA as Map).put(strategy, balanceA + ((simulation.tradeIncrement as Double) * (1 + (simulation.transactionCost as Double))))
                                                (simulation.balancesB as Map).put(strategy, balanceB - ((simulation.tradeIncrement as Double) * correlationAssociation.price))
                                                simulation.tradeCount++
                                            }
                                        }
                                        break
                                    case 'sell':
                                        if (probability > (simulation.sellThreshold as Double)) {
                                            Logger.debug("Selling A for B, p: " + probability)
                                            if (balanceA < (simulation.tradeIncrement as Double) * (1 + (simulation.transactionCost as Double))) {
                                                Logger.debug("Not enough balanceA left: " + balanceA)
                                            } else {
                                                (simulation.balancesA as Map).put(strategy, balanceA - (simulation.tradeIncrement as Double))
                                                (simulation.balancesB as Map).put(strategy, balanceB + proceedsA)
                                                simulation.tradeCount++
                                            }
                                        }
                                        break
                                }
                            }
                        })
                    }
                    threads*.join()
                }
            }
        }

        Map<String, Map> result = [:]
        simulations.each {
            Simulation simulation = it
            (simulation.balancesA as Map).each {
                String strategy = it.key
                Double finalBalance = it.value + ((simulation.balancesA as Map).get(it.key) / finalPrice)
                simulation.result.put(it.key, finalBalance)
                result.put(String.format('%s:%s', simulation.key, strategy), [
                        'balance' : finalBalance,
                        'tradeCount' : simulation.tradeCount
                ])
            }
        }

        Logger.log('results are in')
        Logger.log(result.sort { -it.value.get('balance') }.take(20) as String)

        return result

    }

}
