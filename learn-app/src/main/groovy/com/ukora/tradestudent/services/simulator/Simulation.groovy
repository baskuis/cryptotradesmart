package com.ukora.tradestudent.services.simulator

import java.util.concurrent.ConcurrentHashMap

class Simulation {

    String key
    boolean enabled = true
    Double buyThreshold
    Double sellThreshold
    Double startingBalance
    Double tradeIncrement
    Double transactionCost
    Double finalPrice
    Map<String, Double> tagThresholds = new ConcurrentHashMap<String, Double>()
    Map<String, Double> tagGroupWeights = new ConcurrentHashMap<String, Double>()
    Map balancesA = new ConcurrentHashMap<String, Double>()
    Map balancesB = new ConcurrentHashMap<String, Double>()
    Map tradeCounts = new ConcurrentHashMap<String, Double>()
    Map totalBalances = new ConcurrentHashMap<String, Double>()
    Map pursesEnabled = new ConcurrentHashMap<String, Boolean>()
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
            "tagGroupWeights:%s",
            key,
            buyThreshold,
            sellThreshold,
            startingBalance,
            tradeIncrement,
            transactionCost,
            finalPrice,
            tagGroupWeights
        )
    }

}
