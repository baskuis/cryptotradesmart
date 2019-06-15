package com.ukora.domain.beans.tags.buysell

import com.ukora.domain.beans.tags.AbstractCorrelationTag
import org.springframework.stereotype.Component

@Component
class SellTag extends AbstractCorrelationTag {

    @Override
    String getTagName() {
        'sell'
    }

    @Override
    boolean entry() {
        return false
    }

    @Override
    boolean exit() {
        return true
    }

}
