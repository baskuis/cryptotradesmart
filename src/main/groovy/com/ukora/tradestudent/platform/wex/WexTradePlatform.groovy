package com.ukora.tradestudent.platform.wex

import com.ukora.tradestudent.currencies.pair.CurrencyPair
import com.ukora.tradestudent.currencies.pair.impl.BtcUsdCurrencyPair
import com.ukora.tradestudent.entities.Details
import com.ukora.tradestudent.entities.Exchange
import com.ukora.tradestudent.platform.OrderBook
import com.ukora.tradestudent.platform.OrderBookEntry
import com.ukora.tradestudent.platform.TradePlatform
import com.ukora.tradestudent.strategy.trading.TradeExecution
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

import javax.annotation.PostConstruct

@Component
class WexTradePlatform implements TradePlatform {

    Map<CurrencyPair, Exchange> exchangeLookup = [:]

    final private String URL_GET_ORDER_BOOK = 'https://wex.nz/api/3/depth/%s?limit=1000'

    final private String API_KEY = ""
    final private String API_SECRET = ""

    @Autowired
    BtcUsdCurrencyPair btcUsdCurrencyPair

    RestTemplate restTemplate = new RestTemplate()

    @Override
    boolean doTrade(TradeExecution tradeExecution){
        false
    }

    @PostConstruct
    void init(){
        exchangeLookup.put(btcUsdCurrencyPair, new Exchange(
                platform: 'wex.nz',
                exchange: 'btc_usd',
                details: new Details(
                        tradecurrency: "btc",
                        pricecurrency: "usd"
                )
        ))
    }

    @Override
    OrderBook getOrderBook(CurrencyPair currencyPair) {
        String exchangeReference = exchangeLookup.get(currencyPair).exchange
        ResponseEntity response = restTemplate.getForEntity(String.format(URL_GET_ORDER_BOOK, exchangeReference), Map)
        OrderBook orderBook = new OrderBook()
        if(response.statusCodeValue == 200 || response.statusCodeValue == 201) {
            response.body[exchangeReference]['asks'].each {
                orderBook.asks << new OrderBookEntry(
                        price: it[0] as Double,
                        quantity: it[1] as Double
                )
            }
            response.body[exchangeReference]['bids'].each {
                orderBook.bids << new OrderBookEntry(
                        price: it[0] as Double,
                        quantity: it[1] as Double
                )
            }
        }
        return orderBook
    }

    @Override
    List<CurrencyPair> getSupportedCurrencies() {
        exchangeLookup.keySet()
    }

}
