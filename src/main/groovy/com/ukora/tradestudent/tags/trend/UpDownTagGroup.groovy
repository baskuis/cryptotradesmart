package com.ukora.tradestudent.tags.trend

import com.ukora.tradestudent.tags.AbstractCorrelationTag
import com.ukora.tradestudent.tags.TagGroup
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/** @Component TODO: Need to be able to handle multiple tag groups for p-value calculations */
class UpDownTagGroup implements TagGroup {

    @Autowired
    UpTag upTag

    @Autowired
    DownTag downTag

    List<? extends AbstractCorrelationTag> tags(){
        [upTag, downTag]
    }

    @Override
    boolean applies(String toTag) {
        return tags().find({ it.getTagName() == toTag })
    }

}
