package com.ukora.tradestudent.services.simulator.combined

import com.fasterxml.jackson.databind.ObjectMapper
import com.ukora.domain.beans.trade.TradeExecution
import com.ukora.domain.entities.CorrelationAssociation
import com.ukora.domain.entities.ExtractedText
import com.ukora.domain.entities.SimulationResult
import com.ukora.domain.entities.TextCorrelationAssociation
import com.ukora.tradestudent.services.BytesFetcherService
import com.ukora.tradestudent.services.EmailService
import com.ukora.tradestudent.services.ProbabilityCombinerService
import com.ukora.tradestudent.services.SimulationResultService
import com.ukora.tradestudent.services.simulator.AbstractTradingHistoricalSimulatorService
import com.ukora.tradestudent.services.simulator.CombinedSimulation
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
    EmailService emailService

    @Autowired
    ConcurrentTextAssociationProbabilityService concurrentTextAssociationProbabilityService

    @Autowired
    SimulationResultService simulationResultService

    ObjectMapper objectMapper = new ObjectMapper()

    private final static Double MAX_TRADE_INCREMENT = 0.2
    private final static Double TRADE_INCREMENT = 0.1
    private final static Double TRADE_TRANSACTION_COST = 0.0020
    private final static Double LOWEST_THRESHOLD = 0.4850
    private final static Double HIGHEST_THRESHOLD = 0.5300
    private final static Double THRESHOLD_INCREMENT = 0.0050
    private final static Double MAX_THRESHOLD_DELTA = 0.0200
    private final static Double MIN_NUMERICAL_WEIGHT = -0.4
    private final static Double MAX_NUMERICAL_WEIGHT = 1.2
    private final static Double MIN_TEXT_WEIGHT = -0.4
    private final static Double MAX_TEXT_WEIGHT = 1.2
    private final static Double NUMERICAL_WEIGHT_INC = 0.2

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

        combinedSimulations = []

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
         * Get best text news simulation
         */
        SimulationResult textNewsSimulationResult = simulationResultService.getTopPerformingTextFlexSimulation(ExtractedText.TextSource.NEWS)

        /**
         * Get best text twitter simulation
         */
        SimulationResult textTwitterSimulationResult = simulationResultService.getTopPerformingTextFlexSimulation(ExtractedText.TextSource.TWITTER)


        if (!numericalSimulationResult || !textTwitterSimulationResult || !textNewsSimulationResult) {
            Logger.log('[CRITICAL] Missing one of more top simulation results for combined simulation')
            return
        }

        /**
         * Build simulations
         *
         */
        combinedTradeExecutionStrategyMap.each {
            String combinedTradeExecutionStrategy = it.key
            for (Double numericalWeight = simulationSettings.minNumericalWeight; numericalWeight <= simulationSettings.maxNumericalWeight; numericalWeight += simulationSettings.tagGroupInc) {
                for (Double textNewsWeight = simulationSettings.minTextWeight; textNewsWeight <= simulationSettings.maxTextWeight; textNewsWeight += simulationSettings.tagGroupInc) {
                    for (Double textTwitterWeight = simulationSettings.minTextWeight; textTwitterWeight <= simulationSettings.maxTextWeight; textTwitterWeight += simulationSettings.tagGroupInc) {
                        for (Double tradeIncrement = simulationSettings.tradeIncrement; tradeIncrement <= simulationSettings.maxTradeIncrement; tradeIncrement += simulationSettings.tradeIncrement) {
                            for (Double thresholdBuy = simulationSettings.lowestThreshold; thresholdBuy <= simulationSettings.highestThreshold; thresholdBuy += simulationSettings.thresholdIncrement) {
                                Double maxBuyThreshold = (thresholdBuy + simulationSettings.maxThresholdDelta < simulationSettings.highestThreshold) ? thresholdBuy + simulationSettings.maxThresholdDelta : simulationSettings.highestThreshold
                                Double minBuyThreshold = (thresholdBuy - simulationSettings.maxThresholdDelta > simulationSettings.lowestThreshold) ? thresholdBuy - simulationSettings.maxThresholdDelta : simulationSettings.lowestThreshold
                                for (Double thresholdSell = minBuyThreshold; thresholdSell <= maxBuyThreshold; thresholdSell += simulationSettings.thresholdIncrement) {
                                    combinedSimulations << new CombinedSimulation(
                                            key: String.format(
                                                    "b:%s,s:%s,i:%s,nw:%s,tnw:%s,ttw:%s",
                                                    thresholdBuy,
                                                    thresholdSell,
                                                    tradeIncrement,
                                                    combinedTradeExecutionStrategy,
                                                    numericalWeight,
                                                    textNewsWeight,
                                                    textTwitterWeight,
                                            ),
                                            combinedTradeExecutionStrategy: combinedTradeExecutionStrategy,
                                            probabilityCombinerStrategy: 'weights',
                                            numericalWeight: numericalWeight,
                                            textNewsWeight: textNewsWeight,
                                            tradeIncrement: tradeIncrement,
                                            textTwitterWeight: textTwitterWeight,
                                            buyThreshold: thresholdBuy,
                                            numericalSimulation: numericalSimulationResult,
                                            textNewsSimulation: textNewsSimulationResult,
                                            textTwitterSimulation: textTwitterSimulationResult,
                                            sellThreshold: thresholdSell,
                                            transactionCost: TRADE_TRANSACTION_COST,
                                            balanceA: STARTING_BALANCE,
                                            balanceB: 0D,
                                            tradeCount: 0
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
    runSimulation(Date fromDate) {
        try {
            if (!fromDate) return
            if (simulationRunning) {
                Logger.log("There is already a simulation running. Not starting flex simulation.")
                return
            }
            def partitioned = multiThreadingEnabled ? (0..<numCores).collect {
                combinedSimulations[(it..<combinedSimulations.size()).step(numCores)]
            } : [combinedSimulations]
            if (multiThreadingEnabled) Logger.debug(String.format("Delegating simulation to %s threads", numCores))
            if (multiThreadingEnabled) Logger.debug(String.format("Split up simulations into %s groups", partitioned?.size()))
            simulationRunning = true
            resetSimulations()
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

                List<Thread> threads = []
                partitioned.each { group ->
                    threads << Thread.start({
                        group.findAll { it.enabled }.each {
                            CombinedSimulation simulation = it
                            simulation.finalPrice = correlationAssociation.price
                            CombinedTradeExecutionStrategy combinedTradeExecutionStrategy = combinedTradeExecutionStrategyMap.get(simulation.combinedTradeExecutionStrategy)
                            if (!combinedTradeExecutionStrategy) {
                                Logger.log(String.format('Not able to find %s', combinedTradeExecutionStrategy))
                            }
                            Double balanceProportion = (correlationAssociation.price) ? (
                                    simulation.balanceA /
                                            (simulation.balanceA + (simulation.balanceB / correlationAssociation.price))
                            ) : 1
                            if (!NerdUtils.assertRange(balanceProportion)) {
                                Logger.log(String.format("balanceProportion %s is out of range", balanceProportion))
                                return
                            }
                            TradeExecution tradeExecution = combinedTradeExecutionStrategy.getTrade(
                                    correlationAssociation,
                                    textCorrelationAssociation,
                                    simulation,
                                    balanceProportion)

                            /** execute trade */
                            if (tradeExecution) {
                                simulateTrade(simulation, tradeExecution)
                            }
                        }
                    })
                }

                /** Collect results */
                threads*.join()

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
                    SimulationResult.ExecutionType.COMBINED
            )

        } catch (Exception e) {
            Logger.log('[CRITICAL] Unable to complete simulation. Info:' + e.message)
            e.printStackTrace()
            emailService.sendEmail('[CRITICAL] Unable to complete combined simulation. Info:' + e.message, e.stackTrace.join("\n"), 'baskuis1@gmail.com')
        } finally {

            /** Allow another simulation to be started */
            simulationRunning = false

        }
    }

    /**
     * Extract results from simulations
     * organized into a map for output and persistence
     *
     * @return
     */
    protected Map<String, Map> extractResult() {
        Map<String, Map> result = [:]
        combinedSimulations.each {
            CombinedSimulation simulation = it
            String probabilityCombinerStrategy = 'combinedWeights'
            String tradeExecutionStrategy = it.combinedTradeExecutionStrategy
            result.put(String.format('%s:%s', simulation.key, it.key), [
                    'probabilityCombinerStrategy': probabilityCombinerStrategy,
                    'tradeExecutionStrategy'     : tradeExecutionStrategy,
                    'balance'                    : it.totalBalance,
                    'simulation'                 : simulation,
                    'purseKey'                   : it.key
            ])
        }
        return result
    }

    /**
     * Capture the final results
     *
     * @return
     */
    protected Map<String, Map> captureResults() {
        Logger.log('results are in')
        Map<String, Map> finalResults = extractResult().sort { -it.value.get('balance') }.take(STORE_NUMBER_OF_RESULTS)
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
    static simulateTrade(CombinedSimulation simulation, TradeExecution tradeExecution) {
        if (!tradeExecution) return
        if (tradeExecution.amount < 0) {
            Logger.debug(String.format("Ignoring %s trade execution with negative amount %s on date %s, buy:%s, sell:%s",
                    tradeExecution.tradeType,
                    tradeExecution.amount,
                    tradeExecution.date,
                    simulation.buyThreshold,
                    simulation.sellThreshold
            ))
            return
        }
        Double balanceA = simulation.balanceA
        Double balanceB = simulation.balanceB
        switch (tradeExecution.tradeType) {
            case TradeExecution.TradeType.BUY:
                Double maxAmount = balanceB / tradeExecution.price
                Double amount
                Double newBalanceA
                Double newBalanceB
                if (balanceB > tradeExecution.amount * tradeExecution.price) {
                    amount = tradeExecution.amount * (1 - simulation.transactionCost)
                    newBalanceA = balanceA + amount
                    newBalanceB = balanceB - (tradeExecution.amount * tradeExecution.price)
                } else {
                    amount = maxAmount * (1 - simulation.transactionCost)
                    newBalanceA = balanceA + amount
                    newBalanceB = balanceB - (maxAmount * tradeExecution.price)
                }
                if (amount < MINIMUM_AMOUNT) {
                    Logger.debug(String.format("Not enough balanceB:%s left to buy amount:%s ", balanceB, amount))
                } else {
                    simulation.balanceA = newBalanceA
                    simulation.balanceB = newBalanceB
                    simulation.tradeCount++
                    simulation.totalBalance = newBalanceA + (newBalanceB / tradeExecution.price)
                    Logger.debug(String.format('On %s performing BUY at price:%s Had a:%s, b:%s now have a:%s, b:%s',
                            tradeExecution.date,
                            tradeExecution.price,
                            balanceA,
                            balanceB,
                            newBalanceA,
                            newBalanceB)
                    )
                }
                break
            case TradeExecution.TradeType.SELL:
                Double amount
                Double newBalanceA
                Double newBalanceB
                if (balanceA > tradeExecution.amount) {
                    amount = tradeExecution.amount * (1 - simulation.transactionCost)
                    newBalanceA = balanceA - tradeExecution.amount
                    newBalanceB = balanceB + (amount * tradeExecution.price)
                } else {
                    amount = balanceA * (1 - simulation.transactionCost)
                    newBalanceA = 0
                    newBalanceB = balanceB + (amount * tradeExecution.price)
                }
                if (amount < MINIMUM_AMOUNT) {
                    Logger.debug(String.format("Not enough balanceA:%s left ", balanceA))
                } else {
                    simulation.balanceA = newBalanceA
                    simulation.balanceB = newBalanceB
                    simulation.tradeCount++
                    simulation.totalBalance = newBalanceA + (newBalanceB / tradeExecution.price)
                    Logger.debug(String.format('On %s performing SELL at price:%s Had a:%s, b:%s now have a:%s, b:%s',
                            tradeExecution.date,
                            tradeExecution.price,
                            balanceA,
                            balanceB,
                            newBalanceA,
                            newBalanceB)
                    )
                }
                break
        }
    }

    /**
     * Persist result of simulation
     *
     * @param finalResults
     * @param fromDate
     * @param endDate
     */
    protected void persistSimulationResults(Map<String, Map> finalResults, Date fromDate, Date endDate, SimulationResult.ExecutionType executionType) {
        finalResults.each {
            CombinedSimulation simulation = (it.value.get('simulation') as CombinedSimulation)
            SimulationResult simulationResult = new SimulationResult(
                    executionType: executionType,
                    differential: (it.value.get('balance') as Double) / STARTING_BALANCE,
                    startDate: fromDate,
                    endDate: endDate,
                    tradeIncrement: simulation.tradeIncrement,
                    tradeExecutionStrategy: it.value.get('tradeExecutionStrategy'),
                    probabilityCombinerStrategy: it.value.get('probabilityCombinerStrategy'),
                    buyThreshold: simulation.buyThreshold,
                    sellThreshold: simulation.sellThreshold,
                    tradeCount: simulation.tradeCount,
                    totalValue: simulation.totalBalance,
                    numericalWeight: simulation.numericalWeight,
                    textNewsWeight: simulation.textNewsWeight,
                    textTwitterWeight: simulation.textTwitterWeight,
                    numericalSimulation: simulation.numericalSimulation,
                    textTwitterSimulation: simulation.textTwitterSimulation,
                    textNewsSimulation: simulation.textNewsSimulation
            )
            bytesFetcherService.saveSimulation(simulationResult)
        }
    }

    /**
     * Reset simulation balances
     *
     */
    void resetSimulations() {
        combinedSimulations.each {
            it.enabled = true
            it.balanceA = STARTING_BALANCE
            it.balanceB = 0D
        }
    }

}

