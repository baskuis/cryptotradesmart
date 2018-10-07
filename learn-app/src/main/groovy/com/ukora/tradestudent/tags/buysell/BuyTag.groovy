package com.ukora.tradestudent.tags.buysell

import com.ukora.tradestudent.tags.AbstractCorrelationTag
import org.springframework.stereotype.Component

@Component
class BuyTag extends AbstractCorrelationTag {

    @Override
    String getTagName() {
        'buy'
    }

}
