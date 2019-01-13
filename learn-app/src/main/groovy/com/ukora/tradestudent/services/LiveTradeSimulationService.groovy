package com.ukora.tradestudent.services

import com.ukora.domain.entities.CorrelationAssociation
import com.ukora.domain.entities.Memory
import com.ukora.domain.entities.Metadata
import com.ukora.domain.entities.SimulatedTradeEntry
import com.ukora.domain.entities.SimulationResult
import com.ukora.tradestudent.services.simulator.BuySellTradingHistoricalSimulatorService
import com.ukora.tradestudent.services.simulator.Simulation
import com.ukora.domain.beans.trade.TradeExecution
import com.ukora.tradestudent.strategy.trading.TradeExecutionStrategy
import com.ukora.domain.beans.tags.buysell.BuySellTagGroup
import com.ukora.tradestudent.utils.Logger
import com.ukora.tradestudent.utils.NerdUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class LiveTradeSimulationService {

    private static TRANSACTION_COST = 0.002

    private static Double MINIMUM_AMOUNT = 0.01

    @Autowired
    BytesFetcherService bytesFetcherService

    @Autowired
    ProbabilityCombinerService probabilityCombinerService

    @Autowired
    SimulationResultService simulationResultService

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    BuySellTagGroup buySellTagGroup

    @PostConstruct
    void init() {

        /**
         * Insert initial simulation entry
         */
        SimulatedTradeEntry latestSimulatedTradeEntry = bytesFetcherService.getLatestSimulatedTradeEntry()
        if (!latestSimulatedTradeEntry) {
            Memory memory = bytesFetcherService.getMemory(Date.from(Instant.now().minus(5, ChronoUnit.MINUTES)))
            latestSimulatedTradeEntry = new SimulatedTradeEntry(
                    metadata: new Metadata(
                            datetime: new Date(),
                            hostname: InetAddress.getLocalHost().getHostName()
                    ),
                    balanceA: BuySellTradingHistoricalSimulatorService.STARTING_BALANCE,
                    balanceB: 0,
                    totalValueA: BuySellTradingHistoricalSimulatorService.STARTING_BALANCE,
                    tradeType: TradeExecution.TradeType.BUY,
                    date: new Date(),
                    price: memory?.graph?.price
            )
            bytesFetcherService.insertSimulatedTradeEntry(latestSimulatedTradeEntry)
        }

    }

    @Scheduled(cron = "5 * * * * *")
    @Async
    void simulateTrade() {
        try {
            Date now = new Date()
            List<TradeExecution> tradeExecutions = []
            SimulationResult simulationResult = simulationResultService.getTopPerformingSimulation()
            CorrelationAssociation correlationAssociation = probabilityCombinerService.getCorrelationAssociations(now)
            if (simulationResult) {
                if (correlationAssociation) {
                    TradeExecutionStrategy tradeExecutionStrategy = applicationContext.getBean(simulationResult.tradeExecutionStrategy, TradeExecutionStrategy)
                    String probabilityCombinerStrategy = simulationResult.probabilityCombinerStrategy
                    buySellTagGroup.tags().each {
                        String tag = it.tagName
                        Double probability = correlationAssociation.tagProbabilities.get(probabilityCombinerStrategy).get(tag)
                        if (probability) {
                            SimulatedTradeEntry latestSimulatedTradeEntry = bytesFetcherService.getLatestSimulatedTradeEntry()
                            Double balanceProportion = (
                                    latestSimulatedTradeEntry &&
                                            correlationAssociation.price &&
                                            latestSimulatedTradeEntry.balanceA &&
                                            latestSimulatedTradeEntry.balanceB
                            ) ? latestSimulatedTradeEntry.balanceA / (latestSimulatedTradeEntry.balanceA + (latestSimulatedTradeEntry.balanceB / correlationAssociation.price)) : 1
                            if (!NerdUtils.assertRange(balanceProportion)) {
                                Logger.log(String.format('balanceProportion %s is not in range', balanceProportion))
                                return
                            }
                            TradeExecution tradeExecution = tradeExecutionStrategy.getTrade(
                                    correlationAssociation,
                                    tag,
                                    probability,
                                    new Simulation(
                                            startingBalance: BuySellTradingHistoricalSimulatorService.STARTING_BALANCE,
                                            buyThreshold: simulationResult.buyThreshold,
                                            sellThreshold: simulationResult.sellThreshold,
                                            tradeIncrement: simulationResult.tradeIncrement
                                    ),
                                    probabilityCombinerStrategy,
                                    balanceProportion
                            )
                            if (tradeExecution) tradeExecutions << tradeExecution
                        }
                    }
                    if (tradeExecutions) {
                        Logger.log(String.format("Simulating %s trades", tradeExecutions.size()))
                        simulateTrades(tradeExecutions)
                    } else {
                        Logger.log(String.format("Not simulating trades for %s", now))
                    }
                    return
                }
            } else {
                simulateTrades([new TradeExecution(
                        date: now,
                        tradeType: TradeExecution.TradeType.BUY,
                        amount: 0.1,
                        price: correlationAssociation.price
                )])
                Logger.log(String.format("Moving to safety", tradeExecutions.size()))
                return
            }
            Logger.log(String.format("Unable to evaluate live trades for %s", now))
        } catch(Exception e) {
            Logger.log(String.format("Unable to simulate live trade due to error: %s", e.message))
        }
    }

    /**
     * Simulate trades
     *
     * @param tradeExecutions
     */
    private void simulateTrades(List<TradeExecution> tradeExecutions) {
        tradeExecutions.each { TradeExecution tradeExecution ->
            Logger.log(String.format(
                    "Capturing simulated trade %s amount: %s, price: %s",
                    tradeExecution.tradeType,
                    tradeExecution.amount,
                    tradeExecution.price,
            ))
            SimulatedTradeEntry latestSimulatedTradeEntry = bytesFetcherService.getLatestSimulatedTradeEntry()
            if (latestSimulatedTradeEntry) {
                SimulatedTradeEntry nextTradeEntry = getNextSimulatedTradeEntry(
                        latestSimulatedTradeEntry,
                        tradeExecution
                )
                if (nextTradeEntry) {
                    Logger.log(String.format(
                            "Inserting trade %s amount: %s, price: %s, balanceA: %s, balanceB: %s, totalValueA: %s, date: %s",
                            nextTradeEntry.tradeType,
                            nextTradeEntry.amount,
                            nextTradeEntry.price,
                            nextTradeEntry.balanceA,
                            nextTradeEntry.balanceB,
                            nextTradeEntry.totalValueA,
                            nextTradeEntry.date
                    ))
                    bytesFetcherService.insertSimulatedTradeEntry(nextTradeEntry)
                }
            }
        }
    }

    /**
     * Get next simulated trade entry
     *
     * @param simulatedTradeEntry
     * @param tradeExecution
     * @return
     */
    static SimulatedTradeEntry getNextSimulatedTradeEntry(SimulatedTradeEntry simulatedTradeEntry, TradeExecution tradeExecution) {
        switch (tradeExecution.tradeType) {
            case TradeExecution.TradeType.SELL:
                Double amount
                Double newBalanceA
                Double newBalanceB
                if (simulatedTradeEntry.balanceA > tradeExecution.amount) {
                    amount = tradeExecution.amount * (1 - TRANSACTION_COST)
                    newBalanceA = simulatedTradeEntry.balanceA - tradeExecution.amount
                    newBalanceB = simulatedTradeEntry.balanceB + (amount * tradeExecution.price)
                } else {
                    amount = simulatedTradeEntry.balanceA * (1 - TRANSACTION_COST)
                    newBalanceA = 0
                    newBalanceB = simulatedTradeEntry.balanceB + (amount * tradeExecution.price)
                }
                if (amount < MINIMUM_AMOUNT) return null
                Metadata metadata = simulatedTradeEntry.metadata
                if (!metadata) metadata = new Metadata()
                metadata.datetime = new Date()
                metadata.hostname = InetAddress.getLocalHost().getHostName()
                return new SimulatedTradeEntry(
                        metadata: metadata,
                        tradeType: TradeExecution.TradeType.BUY,
                        amount: tradeExecution.amount,
                        price: tradeExecution.price,
                        balanceA: newBalanceA,
                        balanceB: newBalanceB,
                        totalValueA: newBalanceA + (newBalanceB / tradeExecution.price),
                        date: new Date()
                )
                break
            case TradeExecution.TradeType.BUY:
                Double maxAmount = simulatedTradeEntry.balanceB / tradeExecution.price
                Double amount
                Double newBalanceA
                Double newBalanceB
                if (simulatedTradeEntry.balanceB > (tradeExecution.amount * tradeExecution.price)) {
                    amount = tradeExecution.amount * (1 - TRANSACTION_COST)
                    newBalanceA = simulatedTradeEntry.balanceA + amount
                    newBalanceB = simulatedTradeEntry.balanceB - (tradeExecution.amount * tradeExecution.price)
                } else {
                    amount = maxAmount * (1 - TRANSACTION_COST)
                    newBalanceA = simulatedTradeEntry.balanceA + amount
                    newBalanceB = simulatedTradeEntry.balanceB - (maxAmount * tradeExecution.price)
                }
                if (amount < MINIMUM_AMOUNT) return null
                Metadata metadata = simulatedTradeEntry.metadata
                if (!metadata) metadata = new Metadata()
                metadata.datetime = new Date()
                metadata.hostname = InetAddress.getLocalHost().getHostName()
                return new SimulatedTradeEntry(
                        metadata: metadata,
                        tradeType: TradeExecution.TradeType.SELL,
                        amount: tradeExecution.amount,
                        price: tradeExecution.price,
                        balanceA: newBalanceA,
                        balanceB: newBalanceB,
                        totalValueA: newBalanceA + (newBalanceB / tradeExecution.price),
                        date: new Date()
                )
                break
        }
        return null
    }

}
