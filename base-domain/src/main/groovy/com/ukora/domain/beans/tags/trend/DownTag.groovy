package com.ukora.domain.beans.tags.trend

import com.ukora.domain.beans.tags.AbstractCorrelationTag
import org.springframework.stereotype.Component

@Component
class DownTag extends AbstractCorrelationTag {

    @Override
    String getTagName() {
        return "down"
    }

}
