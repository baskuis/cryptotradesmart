package com.ukora.domain.beans.trade

class TradeExecution {

    enum TradeType {BUY, SELL}

    Date date
    TradeType tradeType
    Double amount
    Double price

}
