package com.ukora.domain.beans.tags.buysell

import com.ukora.domain.beans.tags.AbstractCorrelationTag
import org.springframework.stereotype.Component

@Component
class BuyTag extends AbstractCorrelationTag {

    @Override
    String getTagName() {
        'buy'
    }

}
