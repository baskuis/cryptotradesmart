package com.ukora.tradestudent.services.simulator.combined

import com.ukora.domain.beans.trade.TradeExecution
import com.ukora.domain.entities.CorrelationAssociation
import com.ukora.domain.entities.SimulationResult
import com.ukora.domain.entities.TextCorrelationAssociation
import com.ukora.tradestudent.services.BytesFetcherService
import com.ukora.tradestudent.services.ProbabilityCombinerService
import com.ukora.tradestudent.services.SimulationResultService
import com.ukora.tradestudent.services.simulator.AbstractTradingHistoricalSimulatorService
import com.ukora.tradestudent.services.simulator.CombinedSimulation
import com.ukora.tradestudent.services.simulator.Simulation
import com.ukora.tradestudent.services.text.ConcurrentTextAssociationProbabilityService
import com.ukora.tradestudent.strategy.probability.ProbabilityCombinerStrategy
import com.ukora.tradestudent.strategy.trading.combined.CombinedTradeExecutionStrategy
import com.ukora.tradestudent.utils.Logger
import com.ukora.tradestudent.utils.NerdUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import java.time.Duration
import java.time.Instant

@Service
class CombinedTradingHistoricalSimulatorService extends AbstractTradingHistoricalSimulatorService {

    @Autowired
    BytesFetcherService bytesFetcherService

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    ProbabilityCombinerService probabilityCombinerService

    @Autowired
    ConcurrentTextAssociationProbabilityService concurrentTextAssociationProbabilityService

    @Autowired
    SimulationResultService simulationResultService

    private final static Double MAX_TRADE_INCREMENT = 0.2
    private final static Double TRADE_INCREMENT = 0.1
    private final static Double TRADE_TRANSACTION_COST = 0.0020
    private final static Double LOWEST_THRESHOLD = 0.5000
    private final static Double HIGHEST_THRESHOLD = 0.5300
    private final static Double THRESHOLD_INCREMENT = 0.0050
    private final static Double MAX_THRESHOLD_DELTA = 0.0100
    private final static Double MIN_NUMERICAL_WEIGHT = -0.3
    private final static Double MAX_NUMERICAL_WEIGHT = 1.2
    private final static Double MIN_TEXT_WEIGHT = -0.3
    private final static Double MAX_TEXT_WEIGHT = 1.2
    private final static Double NUMERICAL_WEIGHT_INC = 0.3

    static class SimulationSettings {
        Double tradeIncrement = TRADE_INCREMENT
        Double maxTradeIncrement = MAX_TRADE_INCREMENT
        Double lowestThreshold = LOWEST_THRESHOLD
        Double highestThreshold = HIGHEST_THRESHOLD
        Double thresholdIncrement = THRESHOLD_INCREMENT
        Double maxThresholdDelta = MAX_THRESHOLD_DELTA
        Double minNumericalWeight = MIN_NUMERICAL_WEIGHT
        Double maxNumericalWeight = MAX_NUMERICAL_WEIGHT
        Double minTextWeight = MIN_TEXT_WEIGHT
        Double maxTextWeight = MAX_TEXT_WEIGHT
        Double tagGroupInc = NUMERICAL_WEIGHT_INC
    }

    SimulationSettings simulationSettings = new SimulationSettings()

    Map<String, CombinedTradeExecutionStrategy> combinedTradeExecutionStrategyMap

    /** List of possible configuration variations */
    List<CombinedSimulation> combinedSimulations = []

    @PostConstruct
    void init() {

        /**
         * Get probability combiner strategies
         */
        probabilityCombinerStrategyMap = applicationContext.getBeansOfType(ProbabilityCombinerStrategy)

        /**
         * Get trade execution strategies
         */
        combinedTradeExecutionStrategyMap = applicationContext.getBeansOfType(CombinedTradeExecutionStrategy)

        /**
         * Get best numerical simulation
         */
        SimulationResult numericalSimulationResult = simulationResultService.getTopPerformingNumericalFlexSimulation()

        /**
         * Get best text simulation
         */
        SimulationResult textSimulationResult = simulationResultService.getTopPerformingTextFlexSimulation()

        /**
         * Build simulations
         *
         */
        for (Double numericalWeight = simulationSettings.minNumericalWeight; numericalWeight <= simulationSettings.maxNumericalWeight; numericalWeight += simulationSettings.tagGroupInc) {
            for (Double textWeight = simulationSettings.minTextWeight; textWeight <= simulationSettings.maxTextWeight; textWeight += simulationSettings.tagGroupInc) {
                for (Double tradeIncrement = simulationSettings.tradeIncrement; tradeIncrement <= simulationSettings.maxTradeIncrement; tradeIncrement += simulationSettings.tradeIncrement) {
                    for (Double thresholdBuy = simulationSettings.lowestThreshold; thresholdBuy <= simulationSettings.highestThreshold; thresholdBuy += simulationSettings.thresholdIncrement) {
                        Double maxBuyThreshold = (thresholdBuy + simulationSettings.maxThresholdDelta < simulationSettings.highestThreshold) ? thresholdBuy + simulationSettings.maxThresholdDelta : simulationSettings.highestThreshold
                        Double minBuyThreshold = (thresholdBuy - simulationSettings.maxThresholdDelta > simulationSettings.lowestThreshold) ? thresholdBuy - simulationSettings.maxThresholdDelta : simulationSettings.lowestThreshold
                        for (Double thresholdSell = minBuyThreshold; thresholdSell <= maxBuyThreshold; thresholdSell += simulationSettings.thresholdIncrement) {
                            combinedSimulations << new CombinedSimulation(
                                    numericalWeight: numericalWeight,
                                    textWeight: textWeight,
                                    buyThreshold: thresholdBuy,
                                    numericalSimulation: numericalSimulationResult,
                                    textSimulation: textSimulationResult,
                                    sellThreshold: thresholdSell,
                                    transactionCost: TRADE_TRANSACTION_COST
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Reset SimulationSettings
     *
     */
    void resetSimulationSettings() {
        simulationSettings = new SimulationSettings()
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
        if (!fromDate) return
        if (simulationRunning) {
            Logger.log("There is already a simulation running. Not starting flex simulation.")
            return
        }
        def newSimulation = true
        def partitioned = multiThreadingEnabled ? (0..<numCores).collect {
            combinedSimulations[(it..<simulations.size()).step(numCores)]
        } : [combinedSimulations]
        if (multiThreadingEnabled) Logger.debug(String.format("Delegating simulation to %s threads", numCores))
        if (multiThreadingEnabled) Logger.debug(String.format("Split up simulations into %s groups", partitioned?.size()))
        simulationRunning = true
        resetSimulations()
        def enabledTradeStrategies = combinedTradeExecutionStrategyMap.findAll { it.value.enabled }
        Instant end = Instant.now()
        Duration gap = Duration.ofSeconds(INTERVAL_SECONDS)
        Instant current = Instant.ofEpochMilli(fromDate.time)
        while (current.isBefore(end)) {
            end = Instant.now()
            current = current + gap
            Date d = Date.from(current)

            /**
             * Get p values for numerical associations
             *
             * and also
             *
             * text associations
             *
             */
            List<Thread> retrieveThreads = []
            CorrelationAssociation correlationAssociation = null
            TextCorrelationAssociation textCorrelationAssociation = null
            retrieveThreads << Thread.start {
                correlationAssociation = probabilityCombinerService.getCorrelationAssociations(d)
            }
            retrieveThreads << Thread.start {
                textCorrelationAssociation = concurrentTextAssociationProbabilityService.getCorrelationAssociations(d)
            }
            retrieveThreads*.join()

            /** check */
            if (!correlationAssociation.price) continue
            if (!textCorrelationAssociation.strategyProbabilities) continue

            correlationAssociation?.tagProbabilities?.each {
                String strategy = it.key
                List<Thread> threads = []
                partitioned.each { group ->
                    threads << Thread.start({
                        group.findAll { it.enabled }.each {
                            CombinedSimulation simulation = it
                            simulation.finalPrice = correlationAssociation.price
                            enabledTradeStrategies.each {
                                String purseKey = String.format('%s:%s', strategy, it.key)
                                Double balanceProportion = (correlationAssociation.price) ? simulation.balanceA ?: STARTING_BALANCE /
                                        (simulation.balanceA ?: STARTING_BALANCE + (simulation.balanceB / correlationAssociation.price)) : 1
                                if (!NerdUtils.assertRange(balanceProportion)) {
                                    Logger.log(String.format("balanceProportion %s is out of range", balanceProportion))
                                    return
                                }
                                TradeExecution tradeExecution

                                /** balance purse - sell half of balance A for B at market price */
                                if (newSimulation) {
                                    tradeExecution = new TradeExecution(
                                            date: correlationAssociation.date,
                                            price: correlationAssociation.price,
                                            tradeType: TradeExecution.TradeType.SELL,
                                            amount: STARTING_BALANCE / 2
                                    )

                                    /** otherwise check strategy */
                                } else {
                                    tradeExecution = it.value.getTrade(
                                            correlationAssociation,
                                            textCorrelationAssociation,
                                            simulation,
                                            strategy,
                                            balanceProportion)

                                }

                                /** execute trade */
                                if (tradeExecution) {
                                    Logger.debug(String.format("key:%s,type:%s", purseKey, tradeExecution.tradeType))
//                                    simulateTrade(
//                                            simulation,
//                                            tradeExecution,
//                                            purseKey
//                                    )
                                }

                            }
                        }
                    })
                }

                /** Collect results */
                threads*.join()
                if (newSimulation) {
                    newSimulation = false
                }

            }
        }

        /** Flip back to false */
        forceCompleteSimulation = false

        /** Capture the results */
        Map<String, Map> finalResults = captureResults()

        /** Persist simulation results */
        persistSimulationResults(
                finalResults,
                fromDate,
                Date.from(end),
                SimulationResult.ExecutionType.FLEX
        )

        /** Allow another simulation to be started */
        simulationRunning = false

    }

    /**
     * Reset simulation balances
     *
     */
    void resetSimulations() {
        combinedSimulations.each {
            it.enabled = true
            it.balanceA = null
            it.balanceB = null
        }
    }

}
