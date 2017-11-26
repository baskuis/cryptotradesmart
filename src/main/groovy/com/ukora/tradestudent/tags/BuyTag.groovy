package com.ukora.tradestudent.tags

import org.springframework.stereotype.Component

@Component
class BuyTag extends AbstractCorrelationTag {

    @Override
    String getTagName() {
        'buy'
    }

}
