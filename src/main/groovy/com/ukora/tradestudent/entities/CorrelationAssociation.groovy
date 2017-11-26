package com.ukora.tradestudent.entities

import com.ukora.tradestudent.tags.AbstractCorrelationTag


class CorrelationAssociation extends AbstractAssociation {

    Map<? extends AbstractCorrelationTag, Double> tagProbabilityReference

}
