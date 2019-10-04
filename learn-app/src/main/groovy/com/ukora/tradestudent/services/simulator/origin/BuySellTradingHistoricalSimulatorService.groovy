package com.ukora.tradestudent.services.simulator.origin

import com.ukora.domain.beans.tags.buysell.BuySellTagGroup
import com.ukora.domain.beans.trade.TradeExecution
import com.ukora.domain.entities.CorrelationAssociation
import com.ukora.domain.entities.SimulationResult
import com.ukora.tradestudent.services.BytesFetcherService
import com.ukora.tradestudent.services.ProbabilityCombinerService
import com.ukora.tradestudent.services.simulator.AbstractTradingHistoricalSimulatorService
import com.ukora.tradestudent.services.simulator.Simulation
import com.ukora.tradestudent.strategy.probability.ProbabilityCombinerStrategy
import com.ukora.tradestudent.strategy.trading.TradeExecutionStrategy
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
class BuySellTradingHistoricalSimulatorService extends AbstractTradingHistoricalSimulatorService {

    @Autowired
    BytesFetcherService bytesFetcherService

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    ProbabilityCombinerService probabilityCombinerService

    Map<String, TradeExecutionStrategy> tradeExecutionStrategyMap

    @Autowired
    BuySellTagGroup buySellTagGroup

    /** List of possible configuration variations */
    List<Simulation> simulations = []

    private final static Double MAX_TRADE_INCREMENT = 1
    private final static Double TRADE_INCREMENT = 0.1
    private final static Double TRADE_TRANSACTION_COST = 0.0020
    private final static Double LOWEST_THRESHOLD = 0.49
    private final static Double HIGHEST_THRESHOLD = 0.65
    private final static Double THRESHOLD_INCREMENT = 0.005
    private final static Double MAX_THRESHOLD_DELTA = 0.045

    static class SimulationSettings {
        Double tradeIncrement = TRADE_INCREMENT
        Double maxTradeIncrement = MAX_TRADE_INCREMENT
        Double lowestThreshold = LOWEST_THRESHOLD
        Double highestThreshold = HIGHEST_THRESHOLD
        Double thresholdIncrement = THRESHOLD_INCREMENT
        Double maxThresholdDelta = MAX_THRESHOLD_DELTA
    }

    SimulationSettings simulationSettings = new SimulationSettings()

    void init() {

        /**
         * Reset simulations
         */
        simulations = []

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
        for (Double tradeIncrement = simulationSettings.tradeIncrement; tradeIncrement <= simulationSettings.maxTradeIncrement; tradeIncrement += simulationSettings.tradeIncrement) {
            for (Double thresholdBuy = simulationSettings.lowestThreshold; thresholdBuy <= simulationSettings.highestThreshold; thresholdBuy += simulationSettings.thresholdIncrement) {
                Double maxBuyThreshold = (thresholdBuy + simulationSettings.maxThresholdDelta < simulationSettings.highestThreshold) ? thresholdBuy + simulationSettings.maxThresholdDelta : simulationSettings.highestThreshold
                Double minBuyThreshold = (thresholdBuy - simulationSettings.maxThresholdDelta > simulationSettings.lowestThreshold) ? thresholdBuy - simulationSettings.maxThresholdDelta : simulationSettings.lowestThreshold
                for (Double thresholdSell = minBuyThreshold; thresholdSell <= maxBuyThreshold; thresholdSell += simulationSettings.thresholdIncrement) {
                    simulations << new Simulation(
                            key: String.format("buy:%s,sell:%s,inc:%s", thresholdBuy, thresholdSell, tradeIncrement),
                            buyThreshold: thresholdBuy,
                            sellThreshold: thresholdSell,
                            startingBalance: STARTING_BALANCE,
                            tradeIncrement: tradeIncrement,
                            transactionCost: TRADE_TRANSACTION_COST,
                            balancesA: [:],
                            balancesB: [:],
                            tradeCounts: [:],
                            totalBalances: [:]
                    )
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
        try {
            if (!fromDate) return
            if (simulationRunning) {
                Logger.log("There is already a simulation running. Not starting simulation.")
                return
            }
            init()
            def partitioned = multiThreadingEnabled ? (0..<numCores).collect {
                simulations[(it..<simulations.size()).step(numCores)]
            } : [simulations]
            if (multiThreadingEnabled) Logger.debug(String.format("Delegating simulation to %s threads", numCores))
            if (multiThreadingEnabled) Logger.debug(String.format("Split up simulations into %s groups", partitioned?.size()))
            simulationRunning = true
            resetSimulations()
            def enabledTradeStrategies = tradeExecutionStrategyMap.findAll { it.value.enabled }
            Instant end = Instant.now()
            Duration gap = Duration.ofSeconds(INTERVAL_SECONDS)
            Instant current = Instant.ofEpochMilli(fromDate.time)
            while (current.isBefore(end)) {
                end = Instant.now()
                current = current + gap

                /** Get probabilities */
                CorrelationAssociation correlationAssociation = probabilityCombinerService.getCorrelationAssociations(Date.from(current))
                correlationAssociation.tagProbabilities.each {
                    String strategy = it.key
                    it.value.each {
                        if (!correlationAssociation.price) return
                        if (!it.value) return
                        String tag = it.key
                        if (!buySellTagGroup.tags().find { it.tagName == tag }) return
                        Double probability = it.value
                        List<Thread> threads = []
                        partitioned.each { group ->
                            threads << Thread.start({
                                group.findAll { it.enabled }.each {

                                    /** Run simulation */
                                    Simulation simulation = it
                                    simulation.finalPrice = correlationAssociation.price
                                    enabledTradeStrategies.each {
                                        String purseKey = String.format('%s:%s', strategy, it.key)
                                        boolean purseEnabled = simulation.pursesEnabled.get(purseKey, true)
                                        if (purseEnabled) {
                                            Double balanceProportion = (correlationAssociation.price) ? simulation.balancesA.getOrDefault(purseKey, STARTING_BALANCE) /
                                                    (simulation.balancesA.getOrDefault(purseKey, STARTING_BALANCE) + (simulation.balancesB.getOrDefault(purseKey, 0) / correlationAssociation.price)) : 1
                                            if (!NerdUtils.assertRange(balanceProportion)) {
                                                Logger.log(String.format("balanceProportion %s is out of range", balanceProportion))
                                                return
                                            }
                                            TradeExecution tradeExecution = it.value.getTrade(
                                                        correlationAssociation,
                                                        tag,
                                                        probability,
                                                        simulation,
                                                        strategy,
                                                        balanceProportion)

                                            /** execute trade */
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

                                    /** Disable simulation when all purses are disabled */
                                    if (simulation.pursesEnabled.collect { it.value }.size() == 0) {
                                        Logger.debug(String.format("Disabling on simulation %s all purses disabled", simulation))
                                        simulation.enabled = false
                                    }

                                }
                            })
                        }
                        threads*.join()
                    }
                }
                if (forceCompleteSimulation) break
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
                    SimulationResult.ExecutionType.BASIC
            )

        } catch (Exception e) {
            Logger.log('[CRITICAL] Unable to complete simulation. Info:' + e.message)
            e.printStackTrace()
        } finally {

            /** Allow another simulation to be started */
            simulationRunning = false

        }

    }

}
