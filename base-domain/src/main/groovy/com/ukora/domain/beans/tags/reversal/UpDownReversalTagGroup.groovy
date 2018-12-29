package com.ukora.domain.beans.tags.reversal

import com.ukora.domain.beans.tags.AbstractCorrelationTag
import com.ukora.domain.beans.tags.TagGroup
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
