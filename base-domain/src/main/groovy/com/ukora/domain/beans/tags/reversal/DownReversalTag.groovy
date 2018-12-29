package com.ukora.domain.beans.tags.reversal

import com.ukora.domain.beans.tags.AbstractCorrelationTag
import org.springframework.stereotype.Component

@Component
class DownReversalTag extends AbstractCorrelationTag {

    @Override
    String getTagName() {
        'downreversal'
    }

}
