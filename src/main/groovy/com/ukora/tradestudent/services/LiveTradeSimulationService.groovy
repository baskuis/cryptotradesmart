package com.ukora.tradestudent.services

import com.ukora.tradestudent.entities.CorrelationAssociation
import com.ukora.tradestudent.entities.Lesson
import com.ukora.tradestudent.entities.SimulatedTradeEntry
import com.ukora.tradestudent.entities.SimulationResult
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

@Service
class LiveTradeSimulationService {

    @Autowired
    ProbabilityFigurerService probabilityFigurerService

    @Autowired
    SimulationResultService simulationResultService

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    BuySellTagGroup buySellTagGroup

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
        Logger.log(String.format("Unable to evaluate life trades for %s", now))
    }

    private void simulateTrades(List<TradeExecution> tradeExecutions){

        /** Get initial/latest balance */
        SimulatedTradeEntry simulatedTradeEntry = new SimulatedTradeEntry()

        tradeExecutions.each { TradeExecution tradeExecution ->
            Logger.log(String.format(
                    "Capturing simulated trade %s amount: %s, price: %s",
                    tradeExecution.tradeType,
                    tradeExecution.price,
                    tradeExecution.amount))
            /** TODO: handle trade */
        }

    }

}
