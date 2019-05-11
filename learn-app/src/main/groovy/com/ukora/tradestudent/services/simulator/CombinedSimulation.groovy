package com.ukora.tradestudent.services.simulator

import com.ukora.domain.entities.SimulationResult

class CombinedSimulation {

    String key
    boolean enabled = true

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

}
