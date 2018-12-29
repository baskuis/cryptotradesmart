package com.ukora.domain.entities

import com.ukora.domain.beans.bayes.numbers.NumberAssociation


class BrainNode {

    String reference
    Map<String, NumberAssociation> tagReference = [:]

}
