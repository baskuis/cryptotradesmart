package com.ukora.tradestudent.tags

import org.springframework.stereotype.Component

@Component
class SellTag extends AbstractCorrelationTag {

    @Override
    String getTagName() {
        'sell'
    }

}
