package com.ukora.tradestudent.currencies.pair

import com.ukora.tradestudent.currencies.Currency

interface CurrencyPair {
    Currency getCurrencyA()
    Currency getCurrencyB()
}