package com.ukora.domain.entities

import org.springframework.data.annotation.Id

class TextCorrelationAssociation {

    @Id
    String id

    Date date

    Map<String, Map<String, Map<String, Double>>> strategyProbabilities = [:]

}
