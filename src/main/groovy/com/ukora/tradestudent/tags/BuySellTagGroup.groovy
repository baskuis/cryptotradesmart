package com.ukora.tradestudent.tags

import org.springframework.stereotype.Component

@Component
class BuySellTagGroup implements TagGroup {

    List<? extends AbstractCorrelationTag> tags(){
        [new BuyTag(), new SellTag()]
    }

    @Override
    boolean applies(String toTag) {
        return tags().find({ it.getTagName() == toTag })
    }

}
