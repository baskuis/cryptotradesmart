package com.ukora.tradestudent.tags.trend

import com.ukora.tradestudent.tags.AbstractCorrelationTag
import org.springframework.stereotype.Component

/** @Component TODO: Need to be able to handle multiple tag groups for p-value calculations */
class UpTag extends AbstractCorrelationTag {

    @Override
    String getTagName() {
        return "up"
    }

}
