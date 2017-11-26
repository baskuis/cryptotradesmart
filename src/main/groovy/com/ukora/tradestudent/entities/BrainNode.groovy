package com.ukora.tradestudent.entities

import com.mongodb.DBObject
import com.ukora.tradestudent.bayes.numbers.NumberAssociation

class BrainNode {

    String id
    String reference
    DBObject obj
    Map<String, NumberAssociation> tagReference = [:]

}
