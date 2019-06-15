package com.ukora.domain.beans.tags.moves

import com.ukora.domain.beans.tags.AbstractCorrelationTag
import org.springframework.stereotype.Component

@Component
class DownMoveTag extends AbstractCorrelationTag {

    @Override
    String getTagName() {
        'downmove'
    }

    @Override
    boolean entry() {
        return false
    }

    @Override
    boolean exit() {
        return true
    }

}
