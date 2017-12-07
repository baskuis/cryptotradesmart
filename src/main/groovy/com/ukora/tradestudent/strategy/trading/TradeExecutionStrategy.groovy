package com.ukora.tradestudent.strategy.trading

/** TODO: Build model for trade execution strategies */
interface TradeExecutionStrategy {

    List<TradeExecution> getTrades(String probablityCombinerStrategy, Double price)

}