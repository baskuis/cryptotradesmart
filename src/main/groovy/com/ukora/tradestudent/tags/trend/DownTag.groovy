package com.ukora.tradestudent.tags.trend

import com.ukora.tradestudent.tags.AbstractCorrelationTag
import org.springframework.stereotype.Component

/*@Component*/
class DownTag extends AbstractCorrelationTag {

    @Override
    String getTagName() {
        return "down"
    }

}
