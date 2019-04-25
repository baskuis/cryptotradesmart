package com.ukora.tradestudent.services.simulator

import com.ukora.domain.entities.CorrelationAssociation
import com.ukora.domain.entities.SimulationResult
import com.ukora.tradestudent.services.BytesFetcherService
import com.ukora.tradestudent.services.ProbabilityCombinerService
import com.ukora.tradestudent.strategy.probability.ProbabilityCombinerStrategy
import com.ukora.domain.beans.trade.TradeExecution
import com.ukora.tradestudent.strategy.trading.TradeExecutionStrategy
import com.ukora.domain.beans.tags.buysell.BuySellTagGroup
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
class BuySellTradingHistoricalSimulatorService {

    /** For better performance - we'll stop losing/hyper simulation */
    private final static MAXIMUM_LOSS_TILL_QUIT = 0.6
    private final static MAXIMUM_TRADES_TILL_QUIT = 20000

    @Autowired
    BytesFetcherService bytesFetcherService

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    ProbabilityCombinerService probabilityCombinerService

    Map<String, ProbabilityCombinerStrategy> probabilityCombinerStrategyMap

    Map<String, TradeExecutionStrategy> tradeExecutionStrategyMap

    private static Double MINIMUM_AMOUNT = 0.01

    public final static int STORE_NUMBER_OF_RESULTS = 20

    public final static long INTERVAL_SECONDS = 60

    public final static int numCores = Runtime.getRuntime().availableProcessors()

    public static boolean multiThreadingEnabled = true

    public static boolean simulationRunning = false

    public static boolean forceCompleteSimulation = false

    @Autowired
    BuySellTagGroup buySellTagGroup

    /** List of possible configuration variations */
    List<Simulation> simulations = []

    public final static Double STARTING_BALANCE = 10
    private final static Double MAX_TRADE_INCREMENT = 1
    private final static Double TRADE_INCREMENT = 0.1
    private final static Double TRADE_TRANSACTION_COST = 0.0020
    private final static Double LOWEST_THRESHOLD = 0.49
    private final static Double HIGHEST_THRESHOLD = 0.75
    private final static Double THRESHOLD_INCREMENT = 0.01
    private final static Double MAX_THRESHOLD_DELTA = 0.05

    static class SimulationSettings {
        Double tradeIncrement = TRADE_INCREMENT
        Double maxTradeIncrement = MAX_TRADE_INCREMENT
        Double lowestThreshold = LOWEST_THRESHOLD
        Double highestThreshold = HIGHEST_THRESHOLD
        Double thresholdIncrement = THRESHOLD_INCREMENT
        Double maxThresholdDelta = MAX_THRESHOLD_DELTA
    }

    SimulationSettings simulationSettings = new SimulationSettings()

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
     * Reset simulation balances
     *
     */
    void resetSimulations() {
        simulations.each {
            it.enabled = true
            it.pursesEnabled = [:]
            it.balancesA = [:]
            it.balancesB = [:]
            it.tradeCounts = [:]
            it.totalBalances = [:]
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
        if (!fromDate) return
        if (simulationRunning) {
            Logger.log("There is already a simulation running")
            return
        }
        def newSimulation = true
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
                    partitioned.collect { group ->
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
                                        TradeExecution tradeExecution

                                        /** balance purse - sell half of balance A for B at market price */
                                        if(newSimulation) {
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
                                                    tag,
                                                    probability,
                                                    simulation,
                                                    strategy,
                                                    balanceProportion)

                                        }

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


                    if (newSimulation) {
                        newSimulation = false
                    }

                }
            }
            newSimulation = false
            if (forceCompleteSimulation) break
        }

        /** Flip back to false */
        forceCompleteSimulation = false

        /** Capture the results */
        Map<String, Map> finalResults = captureResults()

        /** Persist simulation results */
        persistSimulationResults(finalResults, fromDate, Date.from(end))

        /** Allow another simulation to be started */
        simulationRunning = false

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
                    sellThreshold: simulation.sellThreshold,
                    tradeCount: simulation.tradeCounts.get(it.value.get('purseKey'), 0),
                    totalValue: simulation.totalBalances.get(it.value.get('purseKey'), 0)
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
    static simulateTrade(Simulation simulation, TradeExecution tradeExecution, String purseKey) {
        if (!tradeExecution) return
        if (tradeExecution.amount < 0) {
            Logger.debug(String.format("Ignoring %s trade execution with negative amount %s on date %s, %s, buy:%s, sell:%s",
                    tradeExecution.tradeType,
                    tradeExecution.amount,
                    tradeExecution.date,
                    purseKey,
                    simulation.buyThreshold,
                    simulation.sellThreshold
            ))
            return
        }
        Double balanceA = simulation.balancesA.get(purseKey, STARTING_BALANCE) as Double
        Double balanceB = simulation.balancesB.get(purseKey, 0) as Double
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
                    simulation.balancesA.put(purseKey, newBalanceA)
                    simulation.balancesB.put(purseKey, newBalanceB)
                    simulation.tradeCounts.put(purseKey, simulation.tradeCounts.get(purseKey, 0) + 1)
                    simulation.totalBalances.put(purseKey, newBalanceA + (newBalanceB / tradeExecution.price))
                    Logger.debug(String.format('On %s performing BUY at price:%s Had a:%s, b:%s now have a:%s, b:%s',
                            tradeExecution.date,
                            tradeExecution.price,
                            balanceA,
                            balanceB,
                            newBalanceA,
                            newBalanceB))
                    assertSimulationPurseIsHealthy(simulation, purseKey)
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
                    simulation.balancesA.put(purseKey, newBalanceA)
                    simulation.balancesB.put(purseKey, newBalanceB)
                    simulation.tradeCounts.put(purseKey, simulation.tradeCounts.get(purseKey, 0) + 1)
                    simulation.totalBalances.put(purseKey, newBalanceA + (newBalanceB / tradeExecution.price))
                    Logger.debug(String.format('On %s performing SELL at price:%s Had a:%s, b:%s now have a:%s, b:%s',
                            tradeExecution.date,
                            tradeExecution.price,
                            balanceA,
                            balanceB,
                            newBalanceA,
                            newBalanceB))
                    assertSimulationPurseIsHealthy(simulation, purseKey)

                }
                break
        }
    }

    /**
     * Disable purse
     *
     * @param simulation
     * @param purseKey
     */
    private static void assertSimulationPurseIsHealthy(Simulation simulation, String purseKey) {
        if (simulation.totalBalances.get(purseKey) / STARTING_BALANCE < MAXIMUM_LOSS_TILL_QUIT) {
            Logger.debug(String.format("Disabling no longer performing simulation purse %s, loss to great", purseKey))
            simulation.pursesEnabled.put(purseKey, false)
        }
        if (simulation.tradeCounts.get(purseKey) > MAXIMUM_TRADES_TILL_QUIT) {
            Logger.debug(String.format("Disabling no longer performing simulation purse %s, too many trades", purseKey))
            simulation.pursesEnabled.put(purseKey, false)
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
                        'simulation'                 : simulation,
                        'purseKey'                   : it.key
                ])
            }
        }
        return result
    }

}
