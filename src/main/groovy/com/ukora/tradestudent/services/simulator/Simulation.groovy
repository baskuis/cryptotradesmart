package com.ukora.tradestudent.services.simulator

class Simulation {

    String key
    Double buyThreshold
    Double sellThreshold
    Double startingBalance
    Double tradeIncrement
    Double transactionCost
    Double finalPrice
    Integer tradeCount = 0
    Map balancesA = [:]
    Map balancesB = [:]
    Map result = [:]
    Map<String, List<String>> tradeLog = [:]

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
