package com.ukora.tradestudent.tags.trend

import com.ukora.tradestudent.tags.AbstractCorrelationTag
import com.ukora.tradestudent.tags.TagGroup
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class UpDownTagGroup implements TagGroup {

    @Autowired
    UpTag upTag

    @Autowired
    DownTag downTag

    @Override
    String getName() {
        return "updown"
    }

    List<? extends AbstractCorrelationTag> tags(){
        [upTag, downTag]
    }

    @Override
    boolean applies(String toTag) {
        return tags().find({ it.getTagName() == toTag })
    }

}
