package com.ukora.tradestudent.services.simulator


class Simulation {
    String key
    Double buyThreshold
    Double sellThreshold
    Double startingBalance
    Double tradeIncrement
    Double transactionCost
    Integer tradeCount = 0
    Map balancesA = [:]
    Map balancesB = [:]
    Map result = [:]
}
