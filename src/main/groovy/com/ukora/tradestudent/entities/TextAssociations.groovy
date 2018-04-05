package com.ukora.tradestudent.entities

import com.ukora.tradestudent.tags.AbstractCorrelationTag

class TextAssociations {

    Map<AbstractCorrelationTag, List<BrainCount>> tagAssociations = [:]
    List<BrainCount> generalAssociations = []

}
