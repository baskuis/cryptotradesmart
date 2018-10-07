package com.ukora.tradestudent.tags.reversal

import com.ukora.tradestudent.tags.AbstractCorrelationTag
import org.springframework.stereotype.Component

@Component
class DownReversalTag extends AbstractCorrelationTag {

    @Override
    String getTagName() {
        'downreversal'
    }

}
