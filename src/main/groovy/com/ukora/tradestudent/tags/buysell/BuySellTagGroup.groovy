package com.ukora.tradestudent.tags.buysell

import com.ukora.tradestudent.tags.AbstractCorrelationTag
import com.ukora.tradestudent.tags.TagGroup
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BuySellTagGroup implements TagGroup {

    @Autowired
    BuyTag buyTag

    @Autowired
    SellTag sellTag

    List<? extends AbstractCorrelationTag> tags(){
        [buyTag, sellTag]
    }

    @Override
    boolean applies(String toTag) {
        return tags().find({ it.getTagName() == toTag })
    }

}
