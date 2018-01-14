package com.ukora.tradestudent.services.simulator

class Simulation {

    String key
    boolean enabled = true
    Double buyThreshold
    Double sellThreshold
    Double startingBalance
    Double tradeIncrement
    Double transactionCost
    Double finalPrice
    Map balancesA = [:]
    Map balancesB = [:]
    Map tradeCounts = [:]
    Map totalBalances = [:]
    Map result = [:]

    @Override
    String toString() {
        return String.format(
            "key:%s," +
            "buyThreshold:%s," +
            "sellThreshold:%s," +
            "startingBalance:%s," +
            "tradeIncrement:%s," +
            "transactionCost:%s," +
            "finalPrice:%s," +
            "tradeCount:%s",
            key,
            buyThreshold,
            sellThreshold,
            startingBalance,
            tradeIncrement,
            transactionCost,
            finalPrice,
            tradeCount)
    }

}
