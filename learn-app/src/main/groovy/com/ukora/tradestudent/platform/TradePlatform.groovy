package com.ukora.tradestudent.platform

import com.ukora.tradestudent.currencies.pair.CurrencyPair
import com.ukora.tradestudent.strategy.trading.TradeExecution

/**
 * Describes a trade platform
 *
 */
interface TradePlatform {

    OrderBook getOrderBook(CurrencyPair currencyPair)

    List<CurrencyPair> getSupportedCurrencies()

    boolean doTrade(TradeExecution tradeExecution)

}