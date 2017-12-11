package com.ukora.tradestudent.services.simulator

import com.ukora.tradestudent.entities.CorrelationAssociation
import com.ukora.tradestudent.entities.SimulationResult
import com.ukora.tradestudent.services.BytesFetcherService
import com.ukora.tradestudent.services.ProbabilityFigurerService
import com.ukora.tradestudent.strategy.probability.ProbabilityCombinerStrategy
import com.ukora.tradestudent.strategy.trading.TradeExecution
import com.ukora.tradestudent.strategy.trading.TradeExecutionStrategy
import com.ukora.tradestudent.tags.buysell.BuySellTagGroup
import com.ukora.tradestudent.utils.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import java.time.Duration
import java.time.Instant

@Service
class BuySellTradingHistoricalSimulatorService {

    @Autowired
    BytesFetcherService bytesFetcherService

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    ProbabilityFigurerService probabilityFigurerService

    Map<String, ProbabilityCombinerStrategy> probabilityCombinerStrategyMap

    Map<String, TradeExecutionStrategy> tradeExecutionStrategyMap

    public final static long INTERVAL_SECONDS = 60

    public final static int numCores = Runtime.getRuntime().availableProcessors()

    public static boolean multithreadingEnabled = false

    @Autowired
    BuySellTagGroup buySellTagGroup

    /** List of possible configuration variations */
    List<Simulation> simulations = []

    private final static Double STARTING_BALANCE = 10
    private final static Double MAX_TRADE_INCREMENT = 2.4
    private final static Double TRADE_INCREMENT = 0.4
    private final static Double TRADE_TRANSACTION_COST = 0.0022
    private final static Double LOWEST_THRESHOLD = 0.65
    private final static Double HIGHEST_THRESHOLD = 1.00
    private final static Double THRESHOLD_INCREMENT = 0.01

    @PostConstruct
    void init() {

        /**
         * Get probability combiner strategies
         */
        probabilityCombinerStrategyMap = applicationContext.getBeansOfType(ProbabilityCombinerStrategy)

        /**
         * Get trade execution strategies
         */
        tradeExecutionStrategyMap = applicationContext.getBeansOfType(TradeExecutionStrategy)

        /**
         * Build simulations
         */
        for (Double tradeIncrement = TRADE_INCREMENT; tradeIncrement <= MAX_TRADE_INCREMENT; tradeIncrement += TRADE_INCREMENT) {
            for (Double thresholdBuy = LOWEST_THRESHOLD; thresholdBuy <= HIGHEST_THRESHOLD; thresholdBuy += THRESHOLD_INCREMENT) {
                for (Double thresholdSell = LOWEST_THRESHOLD; thresholdSell <= HIGHEST_THRESHOLD; thresholdSell += THRESHOLD_INCREMENT) {
                    simulations << new Simulation(
                            key: String.format("buy:%s,sell:%s,inc:%s", thresholdBuy, thresholdSell, tradeIncrement),
                            buyThreshold: thresholdBuy,
                            sellThreshold: thresholdSell,
                            startingBalance: STARTING_BALANCE,
                            tradeIncrement: tradeIncrement,
                            transactionCost: TRADE_TRANSACTION_COST,
                            tradeCount: 0,
                            balancesA: [:],
                            balancesB: [:]
                    )
                }
            }
        }

    }

    /**
     * Start a trading simulation from the requested date
     * to the present
     *
     * @param fromDate
     * @return
     */
    @Async
    runSimulation(Date fromDate) {
        if (!fromDate) return null
        Instant end = Instant.now()
        Duration gap = Duration.ofSeconds(INTERVAL_SECONDS)
        Instant current = Instant.ofEpochMilli(fromDate.time)
        while (current.isBefore(end)) {
            current = current + gap
            CorrelationAssociation correlationAssociation = probabilityFigurerService.getCorrelationAssociations(Date.from(current))
            correlationAssociation.tagProbabilities.each {
                String strategy = it.key
                it.value.each {
                    if (!correlationAssociation.price) return
                    if (!it.value) return
                    String tag = it.key
                    if (!buySellTagGroup.tags().find { it.tagName == tag }) return
                    Double probability = it.value
                    def partitioned = multithreadingEnabled ? (0..<numCores).collect {
                        simulations[(it..<simulations.size()).step(numCores)]
                    } : [simulations]
                    if (multithreadingEnabled) Logger.debug(String.format("Delegating simulation to %s threads", numCores))
                    partitioned.collect { group ->
                        Thread.start({
                            group.each {
                                Simulation simulation = it
                                simulation.finalPrice = correlationAssociation.price
                                tradeExecutionStrategyMap.each {
                                    String purseKey = String.format('%s:%s', strategy, it.key)
                                    TradeExecution tradeExecution = it.value.getTrade(
                                            correlationAssociation,
                                            tag,
                                            probability,
                                            simulation)
                                    if (tradeExecution) {
                                        Logger.debug(String.format("key:%s,type:%s,probability:%s", purseKey, tradeExecution.tradeType, probability))
                                        simulateTrade(
                                                simulation,
                                                tradeExecution,
                                                purseKey
                                        )
                                    }

                                }
                            }
                        })*.join()
                    }
                }
            }
        }

        /** Capture the results */
        Map<String, Map> finalResults = captureResults()

        /** Persist simulation results */
        persistSimulationResults(finalResults, fromDate, Date.from(end))

    }

    /**
     * Persist result of simulation
     *
     * @param finalResults
     * @param fromDate
     * @param endDate
     */
    private void persistSimulationResults(Map<String, Map> finalResults, Date fromDate, Date endDate) {
        finalResults.each {
            Simulation simulation = (it.value.get('simulation') as Simulation)
            SimulationResult simulationResult = new SimulationResult(
                    differential: (it.value.get('balance') as Double) / STARTING_BALANCE,
                    startDate: fromDate,
                    endDate: endDate,
                    tradeIncrement: simulation.tradeIncrement,
                    tradeExecutionStrategy: it.value.get('tradeExecutionStrategy'),
                    probabilityCombinerStrategy: it.value.get('probabilityCombinerStrategy'),
                    buyThreshold: simulation.buyThreshold,
                    sellThreshold: simulation.sellThreshold
            )
            bytesFetcherService.saveSimulation(simulationResult)
        }
    }

    /**
     * Capture the final results
     *
     * @return
     */
    private Map<String, Map> captureResults() {
        Logger.log('results are in')
        Map<String, Map> finalResults = extractResult().sort { -it.value.get('balance') }.take(20)
        Logger.log(finalResults as String)
        return finalResults
    }

    /**
     * Simulate a trade
     *
     * @param simulation
     * @param tradeExecution
     * @param purseKey
     * @return
     */
    private static simulateTrade(Simulation simulation, TradeExecution tradeExecution, String purseKey) {
        if (!tradeExecution) return
        Double costsA = tradeExecution.amount * tradeExecution.price * (1 + simulation.transactionCost)
        Double proceedsA = tradeExecution.amount * tradeExecution.price * (1 - simulation.transactionCost)
        Double balanceB = simulation.balancesB.get(purseKey, 0) as Double
        Double balanceA = simulation.balancesA.get(purseKey, STARTING_BALANCE) as Double
        switch (tradeExecution.tradeType) {
            case TradeExecution.TradeType.BUY:
                if (balanceB < costsA) {
                    Logger.debug(String.format("Not enough balanceB:%s left to buy costsA:%s ", balanceB, costsA))
                } else {
                    simulation.balancesA.put(purseKey, balanceA + (tradeExecution.amount * (1 + simulation.transactionCost)))
                    simulation.balancesB.put(purseKey, balanceB - (tradeExecution.amount * tradeExecution.price))
                    simulation.tradeCount++
                }
                break
            case TradeExecution.TradeType.SELL:
                if (balanceA < (tradeExecution.amount as Double) * (1 + simulation.transactionCost)) {
                    Logger.debug(String.format("Not enough balanceA:%s left ", balanceA))
                } else {
                    simulation.balancesA.put(purseKey, balanceA - tradeExecution.amount)
                    simulation.balancesB.put(purseKey, balanceB + proceedsA)
                    simulation.tradeCount++
                }
                break
        }
    }

    /**
     * Extract results from simulations
     * organized into a map for output and persistence
     *
     * @return
     */
    private Map<String, Map> extractResult() {
        Map<String, Map> result = [:]
        simulations.each {
            Simulation simulation = it
            simulation.balancesA.each {
                String[] keys = (it.key as String).split(/:/) /** Extract strategies from purseKey */
                if (keys.size() != 2) return
                String probabilityCombinerStrategy = keys[0]
                String tradeExecutionStrategy = keys[1]
                Double finalBalance = it.value + (simulation.balancesA.get(it.key) / simulation.finalPrice)
                simulation.result.put(it.key, finalBalance)
                result.put(String.format('%s:%s', simulation.key, it.key), [
                        'probabilityCombinerStrategy': probabilityCombinerStrategy,
                        'tradeExecutionStrategy'     : tradeExecutionStrategy,
                        'balance'                    : finalBalance,
                        'simulationDescription'      : simulation.toString(),
                        'simulation'                 : simulation
                ])
            }
        }
        return result
    }

}
