package com.ukora.tradestudent.services

import com.ukora.tradestudent.entities.CorrelationAssociation
import com.ukora.tradestudent.entities.Metadata
import com.ukora.tradestudent.entities.SimulatedTradeEntry
import com.ukora.tradestudent.entities.SimulationResult
import com.ukora.tradestudent.services.simulator.BuySellTradingHistoricalSimulatorService
import com.ukora.tradestudent.services.simulator.Simulation
import com.ukora.tradestudent.strategy.trading.TradeExecution
import com.ukora.tradestudent.strategy.trading.TradeExecutionStrategy
import com.ukora.tradestudent.tags.buysell.BuySellTagGroup
import com.ukora.tradestudent.utils.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct

@Service
class LiveTradeSimulationService {

    private static TRANSACTION_COST = 0.002

    @Autowired
    BytesFetcherService bytesFetcherService

    @Autowired
    ProbabilityFigurerService probabilityFigurerService

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
        if(!latestSimulatedTradeEntry){
            latestSimulatedTradeEntry = new SimulatedTradeEntry(
                    metadata: new Metadata(
                            datetime: new Date(),
                            hostname: InetAddress.getLocalHost().getHostName()
                    ),
                    balanceA: BuySellTradingHistoricalSimulatorService.STARTING_BALANCE,
                    balanceB: 0,
                    totalValueA: BuySellTradingHistoricalSimulatorService.STARTING_BALANCE,
                    tradeType: TradeExecution.TradeType.BUY,
                    date: new Date()
            )
            bytesFetcherService.insertSimulatedTradeEntry(latestSimulatedTradeEntry)
        }

    }

    @Scheduled(cron = "5 * * * * *")
    @Async
    void simulateTrade(){
        Date now = new Date()
        List<TradeExecution> tradeExecutions = []
        SimulationResult simulationResult = simulationResultService.getTopPerformingSimulation()
        if(simulationResult) {
            CorrelationAssociation correlationAssociation = probabilityFigurerService.getCorrelationAssociations(now)
            if(correlationAssociation) {
                TradeExecutionStrategy tradeExecutionStrategy = applicationContext.getBean(simulationResult.tradeExecutionStrategy, TradeExecutionStrategy)
                String probabilityCombinerStrategy = simulationResult.probabilityCombinerStrategy
                buySellTagGroup.tags().each {
                    String tag = it.tagName
                    Double probability = correlationAssociation.tagProbabilities.get(probabilityCombinerStrategy).get(tag)
                    if(probability) {
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
                                probabilityCombinerStrategy
                        )
                        if (tradeExecution) tradeExecutions << tradeExecution
                    }
                }
                if(tradeExecutions){
                    Logger.log(String.format("Simulating %s trades", tradeExecutions.size()))
                    simulateTrades(tradeExecutions)
                }else{
                    Logger.log(String.format("Not simulating trades for %s", now))
                }
                return
            }
        }
        Logger.log(String.format("Unable to evaluate live trades for %s", now))
    }

    /**
     * Simulate trades
     *
     * @param tradeExecutions
     */
    private void simulateTrades(List<TradeExecution> tradeExecutions){
        tradeExecutions.each { TradeExecution tradeExecution ->
            Logger.log(String.format(
                    "Capturing simulated trade %s amount: %s, price: %s",
                    tradeExecution.tradeType,
                    tradeExecution.amount,
                    tradeExecution.price,
                    ))
            SimulatedTradeEntry latestSimulatedTradeEntry = bytesFetcherService.getLatestSimulatedTradeEntry()
            if(latestSimulatedTradeEntry) {
                SimulatedTradeEntry nextTradeEntry = getNextSimulatedTradeEntry(
                        latestSimulatedTradeEntry,
                        tradeExecution
                )
                if(nextTradeEntry){
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
    static SimulatedTradeEntry getNextSimulatedTradeEntry(SimulatedTradeEntry simulatedTradeEntry, TradeExecution tradeExecution){
        switch (tradeExecution.tradeType){
            case TradeExecution.TradeType.SELL:
                Double amount = tradeExecution.amount * (1 - TRANSACTION_COST)
                Double newBalanceA = simulatedTradeEntry.balanceA - tradeExecution.amount
                Double newBalanceB = simulatedTradeEntry.balanceB + (amount * tradeExecution.price)
                if(newBalanceA < 0){
                    return null
                }
                Metadata metadata = simulatedTradeEntry.metadata
                if(!metadata) metadata = new Metadata()
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
                Double amount = tradeExecution.amount * (1 - TRANSACTION_COST)
                Double newBalanceA = simulatedTradeEntry.balanceA + amount
                Double newBalanceB = simulatedTradeEntry.balanceB - (tradeExecution.amount * tradeExecution.price)
                if(newBalanceB < 0){
                    return null
                }
                Metadata metadata = simulatedTradeEntry.metadata
                if(!metadata) metadata = new Metadata()
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
