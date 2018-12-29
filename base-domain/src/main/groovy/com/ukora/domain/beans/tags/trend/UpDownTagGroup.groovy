package com.ukora.domain.beans.tags.trend

import com.ukora.domain.beans.tags.AbstractCorrelationTag
import com.ukora.domain.beans.tags.TagGroup
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
