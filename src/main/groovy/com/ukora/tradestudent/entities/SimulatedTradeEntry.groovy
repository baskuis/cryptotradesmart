package com.ukora.tradestudent.entities

import com.ukora.tradestudent.strategy.trading.TradeExecution

class SimulatedTradeEntry {
    Metadata metadata
    Double balanceA
    Double balanceB
    TradeExecution.TradeType tradeType
    Double amount
    Date date
}
