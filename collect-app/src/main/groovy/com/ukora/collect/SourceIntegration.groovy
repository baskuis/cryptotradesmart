package com.ukora.collect

import org.knowm.xchange.Exchange
import org.knowm.xchange.ExchangeFactory
import org.knowm.xchange.binance.BinanceExchange
import org.knowm.xchange.bitfinex.v1.BitfinexExchange
import org.knowm.xchange.bitstamp.BitstampExchange
import org.knowm.xchange.coinbasepro.CoinbaseProExchange
import org.knowm.xchange.currency.CurrencyPair
import org.knowm.xchange.dto.marketdata.OrderBook
import org.knowm.xchange.kraken.KrakenExchange

interface SourceIntegration {

    enum IntegrationType {
        KRAKEN, BINANCE, COINBASEPRO, BITSTAMP, BITFINEX
    }

    boolean enabled()

    IntegrationType integrationType()

    Health health()

    Exchange exchange()

    Book capture(CurrencyPair currencyPair)

    long lastAttempted()

    long lastSuccess()

    static class Book {
        OrderBook orderBook
        IntegrationType integrationType
        Date timestamp
        CurrencyPair currencyPair
    }

    static class Health {
        Health(Status s) { status = s }

        enum Status {
            UP, DOWN, PENDING
        }
        Status status
        String message
    }

    static class BitfinexIntegration extends AbstractIntegration {
        @Override
        boolean enabled() { return false }
        Exchange bitfinexExchange = ExchangeFactory.INSTANCE.createExchange(BitfinexExchange)

        IntegrationType integrationType() { IntegrationType.BITFINEX }

        @Override
        Exchange exchange() { return bitfinexExchange }
    }

    static class BitstampIntegration extends AbstractIntegration {
        @Override
        boolean enabled() { return false }
        Exchange bitstampExchange = ExchangeFactory.INSTANCE.createExchange(BitstampExchange)

        IntegrationType integrationType() { IntegrationType.BITSTAMP }

        @Override
        Exchange exchange() { return bitstampExchange }
    }

    static class KrakenIntegration extends AbstractIntegration {
        @Override
        boolean enabled() { return false }
        Exchange krakenExchange = ExchangeFactory.INSTANCE.createExchange(KrakenExchange)

        IntegrationType integrationType() { IntegrationType.KRAKEN }

        @Override
        Exchange exchange() { return krakenExchange }
    }

    static class BinanceIntegration extends AbstractIntegration {
        @Override
        boolean enabled() { return false }
        Exchange binanceExchange = ExchangeFactory.INSTANCE.createExchange(BinanceExchange)

        IntegrationType integrationType() { IntegrationType.BINANCE }

        @Override
        Exchange exchange() { return binanceExchange }
    }

    static class CoinbaseProIntegration extends AbstractIntegration {
        @Override
        boolean enabled() { return true }
        Exchange coinbaseProExchange = ExchangeFactory.INSTANCE.createExchange(CoinbaseProExchange)

        IntegrationType integrationType() { IntegrationType.COINBASEPRO }

        @Override
        Exchange exchange() { return coinbaseProExchange }

        @Override
        OrderBook getOrderBook(CurrencyPair currencyPair) {
            return exchange().getMarketDataService().getOrderBook(currencyPair, 2)
        }
    }

    static abstract class AbstractIntegration implements SourceIntegration {
        long lastAttempted = 0l
        long lastSuccess = 0l
        Health health = new Health(Health.Status.PENDING)

        OrderBook getOrderBook(CurrencyPair currencyPair) {
            return exchange().getMarketDataService().getOrderBook(currencyPair)
        }

        Book capture(CurrencyPair currencyPair) {
            try {
                lastAttempted = System.currentTimeMillis()
                OrderBook orderBook = getOrderBook(currencyPair)
                Book book = new Book()
                book.orderBook = orderBook
                book.integrationType = integrationType()
                book.timestamp = new Date()
                book.currencyPair = currencyPair
                lastSuccess = System.currentTimeMillis()
                health.status = Health.Status.UP
                return book
            } catch (Exception e) {
                health.status = Health.Status.DOWN
                health.message = e.message
            }
            return null
        }

        @Override
        Health health() { return health }

        @Override
        long lastAttempted() { return lastAttempted }

        @Override
        long lastSuccess() { return lastSuccess }

    }

}