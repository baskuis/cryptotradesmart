package com.ukora.domain.beans.tags.moves

import com.ukora.domain.beans.tags.AbstractCorrelationTag
import org.springframework.stereotype.Component

@Component
class UpMoveTag extends AbstractCorrelationTag {

    @Override
    String getTagName() {
        'upmove'
    }

    @Override
    boolean entry() {
        return true
    }

    @Override
    boolean exit() {
        return false
    }

}

