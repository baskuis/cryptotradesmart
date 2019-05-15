package com.ukora.tradestudent.services.simulator

import com.ukora.domain.beans.trade.TradeExecution
import com.ukora.domain.entities.SimulationResult
import com.ukora.tradestudent.services.BytesFetcherService
import com.ukora.tradestudent.strategy.probability.ProbabilityCombinerStrategy
import com.ukora.tradestudent.utils.Logger

abstract class AbstractTradingHistoricalSimulatorService {

    abstract BytesFetcherService getBytesFetcherService()

    public final static Double STARTING_BALANCE = 10

    public final static Double MINIMUM_AMOUNT = 0.01

    public final static int STORE_NUMBER_OF_RESULTS = 20

    public final static long INTERVAL_SECONDS = 60

    public final static int numCores = Runtime.getRuntime().availableProcessors()

    public static boolean multiThreadingEnabled = true

    public static boolean simulationRunning = false

    public static boolean forceCompleteSimulation = false

    /** For better performance - we'll stop losing/hyper simulation */
    final static MAXIMUM_LOSS_TILL_QUIT = 0.6
    final static MAXIMUM_TRADES_TILL_QUIT = 5000

    Map<String, ProbabilityCombinerStrategy> probabilityCombinerStrategyMap

    /** List of possible configuration variations */
    List<Simulation> simulations = []

    /**
     * Persist result of simulation
     *
     * @param finalResults
     * @param fromDate
     * @param endDate
     */
    protected void persistSimulationResults(Map<String, Map> finalResults, Date fromDate, Date endDate, SimulationResult.ExecutionType executionType) {
        finalResults.each {
            Simulation simulation = (it.value.get('simulation') as Simulation)
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
                    tradeCount: simulation.tradeCounts.get(it.value.get('purseKey') as String, 0d),
                    totalValue: simulation.totalBalances.get(it.value.get('purseKey') as String, 0d),
                    tagGroupWeights: simulation.tagGroupWeights
            )
            bytesFetcherService.saveSimulation(simulationResult)
        }
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
    protected static void assertSimulationPurseIsHealthy(Simulation simulation, String purseKey) {
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
    protected Map<String, Map> extractResult() {
        Map<String, Map> result = [:]
        simulations.each {
            Simulation simulation = it
            simulation.balancesA.each {
                String[] keys = (it.key as String).split(/:/) /** Extract strategies from purseKey */
                if (keys.size() != 2) return
                String probabilityCombinerStrategy = keys[0]
                String tradeExecutionStrategy = keys[1]
                Double finalBalance = simulation.totalBalances.get(it.key)
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

}
