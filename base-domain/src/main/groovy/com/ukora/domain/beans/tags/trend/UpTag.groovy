package com.ukora.domain.beans.tags.trend

import com.ukora.domain.beans.tags.AbstractCorrelationTag
import org.springframework.stereotype.Component

@Component
class UpTag extends AbstractCorrelationTag {

    @Override
    String getTagName() {
        return "up"
    }

}
