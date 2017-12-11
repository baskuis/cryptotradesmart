package com.ukora.tradestudent.platform

import com.ukora.tradestudent.entities.Exchange

class OrderBook {
    Exchange exchange
    List<OrderBookEntry> asks = []
    List<OrderBookEntry> bids = []
}
