package com.ukora.tradestudent.currencies.impl

import com.ukora.tradestudent.currencies.Currency
import org.springframework.stereotype.Component

@Component
class BTCCurrency implements Currency {

    @Override
    String getName() {
        return "Bitcoin"
    }

    @Override
    String getSymbol() {
        return "BTC"
    }

}
