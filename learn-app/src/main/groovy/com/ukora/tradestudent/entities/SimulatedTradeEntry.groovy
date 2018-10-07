package com.ukora.tradestudent.entities

import com.ukora.tradestudent.strategy.trading.TradeExecution

class SimulatedTradeEntry {
    Metadata metadata
    Double totalValueA
    Double balanceA
    Double balanceB
    TradeExecution.TradeType tradeType
    Double amount
    Double price
    Date date
}
