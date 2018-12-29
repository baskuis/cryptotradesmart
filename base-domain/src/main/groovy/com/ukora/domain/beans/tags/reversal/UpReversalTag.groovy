package com.ukora.domain.beans.tags.reversal

import com.ukora.domain.beans.tags.AbstractCorrelationTag
import org.springframework.stereotype.Component

@Component
class UpReversalTag extends AbstractCorrelationTag {

    @Override
    String getTagName() {
        'upreversal'
    }

}
