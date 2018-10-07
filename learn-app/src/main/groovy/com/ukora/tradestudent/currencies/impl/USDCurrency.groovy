package com.ukora.tradestudent.currencies.impl

import com.ukora.tradestudent.currencies.Currency
import org.springframework.stereotype.Component

@Component
class USDCurrency implements Currency {

    @Override
    String getName() {
        return "US Dollar"
    }

    @Override
    String getSymbol() {
        return "USD"
    }

}
