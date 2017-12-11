package com.ukora.tradestudent.platform.wex

import com.ukora.tradestudent.currencies.impl.BTCCurrency
import com.ukora.tradestudent.currencies.impl.USDCurrency
import com.ukora.tradestudent.currencies.pair.impl.BtcUsdCurrencyPair
import com.ukora.tradestudent.platform.OrderBook
import spock.lang.Specification

class WexTradePlatformSpec extends Specification {

    WexTradePlatform wexTradePlatform = new WexTradePlatform()

    BtcUsdCurrencyPair btcUsdCurrencyPair = new BtcUsdCurrencyPair(
            usdCurrency: new USDCurrency(),
            btcCurrency: new BTCCurrency()
    )

    def setup() {
        wexTradePlatform.btcUsdCurrencyPair = btcUsdCurrencyPair
        wexTradePlatform.init()
    }

    def "getOrderBook works"() {

        when:
        OrderBook orderBook =wexTradePlatform.getOrderBook(btcUsdCurrencyPair)

        then:
        orderBook != null
        orderBook.asks.size() > 0
        orderBook.bids.size() > 0

    }

}
