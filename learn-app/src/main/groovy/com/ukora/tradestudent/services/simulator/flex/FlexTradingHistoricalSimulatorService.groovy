package com.ukora.tradestudent.services.simulator.flex

import com.fasterxml.jackson.databind.ObjectMapper
import com.ukora.domain.beans.tags.buysell.BuySellTagGroup
import com.ukora.domain.beans.tags.moves.UpDownMovesTagGroup
import com.ukora.domain.beans.tags.reversal.UpDownReversalTagGroup
import com.ukora.domain.beans.tags.trend.UpDownTagGroup
import com.ukora.domain.beans.trade.TradeExecution
import com.ukora.domain.entities.CorrelationAssociation
import com.ukora.domain.entities.ExtractedText
import com.ukora.domain.entities.SimulationResult
import com.ukora.domain.entities.TextCorrelationAssociation
import com.ukora.tradestudent.services.BytesFetcherService
import com.ukora.tradestudent.services.EmailService
import com.ukora.tradestudent.services.ProbabilityCombinerService
import com.ukora.tradestudent.services.simulator.AbstractTradingHistoricalSimulatorService
import com.ukora.tradestudent.services.simulator.Simulation
import com.ukora.tradestudent.services.text.ConcurrentTextAssociationProbabilityService
import com.ukora.tradestudent.strategy.probability.ProbabilityCombinerStrategy
import com.ukora.tradestudent.strategy.trading.flex.FlexTradeExecutionStrategy
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
class FlexTradingHistoricalSimulatorService extends AbstractTradingHistoricalSimulatorService {

    @Autowired
    BytesFetcherService bytesFetcherService

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    EmailService emailService

    @Autowired
    BuySellTagGroup buySellTagGroup
    @Autowired
    UpDownTagGroup upDownTagGroup
    @Autowired
    UpDownReversalTagGroup upDownReversalTagGroup
    @Autowired
    UpDownMovesTagGroup upDownMovesTagGroup

    @Autowired
    ProbabilityCombinerService probabilityCombinerService

    @Autowired
    ConcurrentTextAssociationProbabilityService concurrentTextAssociationProbabilityService

    ObjectMapper objectMapper = new ObjectMapper()

    private final static Double MAX_TRADE_INCREMENT = 0.1
    private final static Double TRADE_INCREMENT = 0.1
    private final static Double TRADE_TRANSACTION_COST = 0.0020
    private final static Double LOWEST_THRESHOLD = 0.5000
    private final static Double HIGHEST_THRESHOLD = 0.5300
    private final static Double THRESHOLD_INCREMENT = 0.0050
    private final static Double MAX_THRESHOLD_DELTA = 0.0100
    private final static Double MIN_TAG_GROUP_WEIGHT = -0.3
    private final static Double MAX_TAG_GROUP_WEIGHT = 1.2
    private final static Double TAG_GROUP_WEIGHT_INC = 0.3

    static class SimulationSettings {
        Double tradeIncrement = TRADE_INCREMENT
        Double maxTradeIncrement = MAX_TRADE_INCREMENT
        Double lowestThreshold = LOWEST_THRESHOLD
        Double highestThreshold = HIGHEST_THRESHOLD
        Double thresholdIncrement = THRESHOLD_INCREMENT
        Double maxThresholdDelta = MAX_THRESHOLD_DELTA
        Double minTagGroupWeight = MIN_TAG_GROUP_WEIGHT
        Double maxTagGroupWeight = MAX_TAG_GROUP_WEIGHT
        Double tagGroupInc = TAG_GROUP_WEIGHT_INC
    }

    SimulationSettings simulationSettings = new SimulationSettings()

    Map<String, FlexTradeExecutionStrategy> flexTradeExecutionStrategyMap

    @PostConstruct
    void init() {

        /**
         * Get probability combiner strategies
         */
        probabilityCombinerStrategyMap = applicationContext.getBeansOfType(ProbabilityCombinerStrategy)

        /**
         * Get trade execution strategies
         */
        flexTradeExecutionStrategyMap = applicationContext.getBeansOfType(FlexTradeExecutionStrategy)

        /**
         * Build simulations
         *
         */
        for (Double buySellTagGroupWeight = simulationSettings.minTagGroupWeight; buySellTagGroupWeight <= simulationSettings.maxTagGroupWeight; buySellTagGroupWeight += simulationSettings.tagGroupInc) {
            for (Double upDownTagGroupWeight = simulationSettings.minTagGroupWeight; upDownTagGroupWeight <= simulationSettings.maxTagGroupWeight; upDownTagGroupWeight += simulationSettings.tagGroupInc) {
                for (Double upDownReversalTagGroupWeight = simulationSettings.minTagGroupWeight; upDownReversalTagGroupWeight <= simulationSettings.maxTagGroupWeight; upDownReversalTagGroupWeight += simulationSettings.tagGroupInc) {
                    for (Double upDownMovesTagGroupWeight = simulationSettings.minTagGroupWeight; upDownMovesTagGroupWeight <= simulationSettings.maxTagGroupWeight; upDownMovesTagGroupWeight += simulationSettings.tagGroupInc) {
                        for (Double tradeIncrement = simulationSettings.tradeIncrement; tradeIncrement <= simulationSettings.maxTradeIncrement; tradeIncrement += simulationSettings.tradeIncrement) {
                            for (Double thresholdBuy = simulationSettings.lowestThreshold; thresholdBuy <= simulationSettings.highestThreshold; thresholdBuy += simulationSettings.thresholdIncrement) {
                                Double maxBuyThreshold = (thresholdBuy + simulationSettings.maxThresholdDelta < simulationSettings.highestThreshold) ? thresholdBuy + simulationSettings.maxThresholdDelta : simulationSettings.highestThreshold
                                Double minBuyThreshold = (thresholdBuy - simulationSettings.maxThresholdDelta > simulationSettings.lowestThreshold) ? thresholdBuy - simulationSettings.maxThresholdDelta : simulationSettings.lowestThreshold
                                for (Double thresholdSell = minBuyThreshold; thresholdSell <= maxBuyThreshold; thresholdSell += simulationSettings.thresholdIncrement) {
                                    simulations << new Simulation(
                                            key: String.format(
                                                    "b:%s,s:%s,i:%s,bsw:%s,udw:%s,rw:%s,mw:%s",
                                                    thresholdBuy,
                                                    thresholdSell,
                                                    tradeIncrement,
                                                    buySellTagGroupWeight,
                                                    upDownTagGroupWeight,
                                                    upDownReversalTagGroupWeight,
                                                    upDownMovesTagGroupWeight
                                            ),
                                            buyThreshold: thresholdBuy,
                                            sellThreshold: thresholdSell,
                                            startingBalance: STARTING_BALANCE,
                                            tradeIncrement: tradeIncrement,
                                            transactionCost: TRADE_TRANSACTION_COST,
                                            tagGroupWeights: [
                                                    (buySellTagGroup.name)       : buySellTagGroupWeight,
                                                    (upDownTagGroup.name)        : upDownTagGroupWeight,
                                                    (upDownReversalTagGroup.name): upDownReversalTagGroupWeight,
                                                    (upDownMovesTagGroup.name)   : upDownMovesTagGroupWeight,
                                            ]
                                    )
                                }
                            }
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
    runSimulation(Date fromDate, FlexTradeExecutionStrategy.Type type, ExtractedText.TextSource textSource) {
        try {
            if (!fromDate) return
            if (simulationRunning) {
                Logger.log("There is already a simulation running. Not starting flex simulation.")
                return
            }
            def partitioned = multiThreadingEnabled ? (0..<numCores).collect {
                simulations[(it..<simulations.size()).step(numCores)]
            } : [simulations]
            if (multiThreadingEnabled) Logger.debug(String.format("Delegating simulation to %s threads", numCores))
            if (multiThreadingEnabled) Logger.debug(String.format("Split up simulations into %s groups", partitioned?.size()))
            simulationRunning = true
            resetSimulations()
            def enabledTradeStrategies = flexTradeExecutionStrategyMap.findAll {
                it.value.enabled &&
                        (!type || type == it.value.type) &&
                        (!textSource || textSource == it.value.textSource)
            }
            if (!enabledTradeStrategies) {
                Logger.log("No qualified tradeStrategies found.")
                return
            }
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
                                                    textCorrelationAssociation,
                                                    simulation,
                                                    strategy,
                                                    balanceProportion)

                                        /** execute trade */
                                        if (tradeExecution) {
                                            Logger.debug(String.format("key:%s,type:%s", purseKey, tradeExecution.tradeType))
                                            simulateTrade(
                                                    simulation,
                                                    tradeExecution,
                                                    purseKey
                                            )
                                        }

                                    }

                                }
                            }
                        })
                    }

                    /** Collect results */
                    threads*.join()

                }

                /** Force complete simulation if requested */
                if (forceCompleteSimulation) break

            }

            /** Flip back to false */
            forceCompleteSimulation = false

            /** Capture the results */
            Map<String, Map> finalResults = captureResults()

            /** Email the results */
            emailService.sendEmail('Flex trading simulation results are in', objectMapper.writeValueAsString(finalResults), 'baskuis1@gmail.com')

            /** Persist simulation results */
            persistSimulationResults(
                    finalResults,
                    fromDate,
                    Date.from(end),
                    SimulationResult.ExecutionType.FLEX
            )

        } catch (Exception e) {
            Logger.log('[CRITICAL] Unable to complete simulation. Info:' + e.message)
            e.printStackTrace()
            emailService.sendEmail('[CRITICAL] Unable to complete flex simulation. Info:' + e.message, e.stackTrace.join("\n"), 'baskuis1@gmail.com')
        } finally {

            /** Allow another simulation to be started */
            simulationRunning = false

        }

    }

}
