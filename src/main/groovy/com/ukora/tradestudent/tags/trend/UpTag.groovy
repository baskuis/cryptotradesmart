package com.ukora.tradestudent.tags.trend

import com.ukora.tradestudent.tags.AbstractCorrelationTag
import org.springframework.stereotype.Component

@Component
class UpTag extends AbstractCorrelationTag {

    @Override
    String getTagName() {
        return "up"
    }

}
