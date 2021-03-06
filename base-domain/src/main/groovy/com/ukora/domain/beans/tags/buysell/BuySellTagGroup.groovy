package com.ukora.domain.beans.tags.buysell

import com.ukora.domain.beans.tags.AbstractCorrelationTag
import com.ukora.domain.beans.tags.TagGroup
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BuySellTagGroup implements TagGroup {

    @Autowired
    BuyTag buyTag

    @Autowired
    SellTag sellTag

    @Override
    String getName() {
        return "buysell"
    }

    List<? extends AbstractCorrelationTag> tags(){
        [buyTag, sellTag]
    }

    @Override
    boolean applies(String toTag) {
        return tags().find({ it.getTagName() == toTag })
    }

}
