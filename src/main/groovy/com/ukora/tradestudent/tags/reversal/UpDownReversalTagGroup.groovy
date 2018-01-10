package com.ukora.tradestudent.tags.reversal

import com.ukora.tradestudent.tags.AbstractCorrelationTag
import com.ukora.tradestudent.tags.TagGroup
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class UpDownReversalTagGroup implements TagGroup {

    @Autowired
    UpReversalTag upReversalTag

    @Autowired
    DownReversalTag downReversalTag

    @Override
    List<? extends AbstractCorrelationTag> tags() {
        return [upReversalTag, downReversalTag]
    }

    @Override
    String getName() {
        return 'updownreversal'
    }

    @Override
    boolean applies(String toTag) {
        return tags().find({ it.getTagName() == toTag })
    }
}
