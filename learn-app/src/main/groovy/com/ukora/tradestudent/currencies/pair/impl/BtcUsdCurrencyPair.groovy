package com.ukora.tradestudent.currencies.pair.impl

import com.ukora.tradestudent.currencies.Currency
import com.ukora.tradestudent.currencies.impl.BTCCurrency
import com.ukora.tradestudent.currencies.impl.USDCurrency
import com.ukora.tradestudent.currencies.pair.CurrencyPair
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BtcUsdCurrencyPair implements CurrencyPair{

    @Autowired
    BTCCurrency btcCurrency

    @Autowired
    USDCurrency usdCurrency

    @Override
    Currency getCurrencyA() {
        return btcCurrency
    }

    @Override
    Currency getCurrencyB() {
        return usdCurrency
    }

}
