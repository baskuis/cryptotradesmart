package com.ukora.tradestudent.platform.gdax

import com.ukora.tradestudent.currencies.impl.BTCCurrency
import com.ukora.tradestudent.currencies.impl.USDCurrency
import com.ukora.tradestudent.currencies.pair.impl.BtcUsdCurrencyPair
import com.ukora.tradestudent.platform.OrderBook
import spock.lang.Specification

class GdaxTradePlatformSpec extends Specification {

    GdaxTradePlatform gdaxTradePlatform = new GdaxTradePlatform()

    BtcUsdCurrencyPair btcUsdCurrencyPair = new BtcUsdCurrencyPair(
            usdCurrency: new USDCurrency(),
            btcCurrency: new BTCCurrency()
    )

    def setup() {
        gdaxTradePlatform.btcUsdCurrencyPair = btcUsdCurrencyPair
        gdaxTradePlatform.init()
    }

    def "getOrderBook works"() {

        when:
        OrderBook orderBook = gdaxTradePlatform.getOrderBook(btcUsdCurrencyPair)

        then:
        orderBook != null
        orderBook.asks.size() > 0
        orderBook.bids.size() > 0

    }

}
