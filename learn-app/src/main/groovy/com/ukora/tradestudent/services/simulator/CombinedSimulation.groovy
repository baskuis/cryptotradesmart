package com.ukora.tradestudent.services.simulator

import com.ukora.domain.entities.SimulationResult

import java.util.concurrent.ConcurrentHashMap

class CombinedSimulation {

    String key
    boolean enabled = true

    String probabilityCombinerStrategy
    String combinedTradeExecutionStrategy

    SimulationResult numericalSimulation
    SimulationResult textNewsSimulation
    SimulationResult textTwitterSimulation

    Double buyThreshold
    Double sellThreshold

    Double numericalWeight = 1d
    Double textNewsWeight = 1d
    Double textTwitterWeight = 1d

    Double tradeIncrement
    Double transactionCost
    Double finalPrice

    Double tradeCount = 0
    Double balanceA
    Double balanceB
    Double totalBalance

    Map balancesA = new ConcurrentHashMap<String, Double>()
    Map balancesB = new ConcurrentHashMap<String, Double>()
    Map tradeCounts = new ConcurrentHashMap<String, Double>()
    Map totalBalances = new ConcurrentHashMap<String, Double>()
    Map pursesEnabled = new ConcurrentHashMap<String, Boolean>()
    Map result = [:]

}
