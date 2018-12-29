package com.ukora.domain.entities

import com.ukora.domain.beans.trade.TradeExecution

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
