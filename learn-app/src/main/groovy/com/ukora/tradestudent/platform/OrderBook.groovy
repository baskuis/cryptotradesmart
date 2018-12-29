package com.ukora.tradestudent.platform

import com.ukora.domain.entities.Exchange

class OrderBook {
    Exchange exchange
    List<OrderBookEntry> asks = []
    List<OrderBookEntry> bids = []
}
