package com.ukora.domain.beans.tags.moves

import com.ukora.domain.beans.tags.AbstractCorrelationTag
import com.ukora.domain.beans.tags.TagGroup
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class UpDownMovesTagGroup implements TagGroup {

    @Autowired
    UpMoveTag upMoveTag

    @Autowired
    DownMoveTag downMoveTag

    @Override
    String getName() {
        return "updownmove"
    }

    List<? extends AbstractCorrelationTag> tags() {
        [upMoveTag, downMoveTag]
    }

    @Override
    boolean applies(String toTag) {
        return tags().find({ it.getTagName() == toTag })
    }

}

