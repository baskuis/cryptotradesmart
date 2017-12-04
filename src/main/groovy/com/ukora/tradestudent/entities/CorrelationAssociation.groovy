package com.ukora.tradestudent.entities

import com.ukora.tradestudent.bayes.numbers.NumberAssociationProbability

class CorrelationAssociation extends AbstractAssociation {

    Map<String, Double> numericAssociations = [:]
    Map<String, Map<String, NumberAssociationProbability>> numericAssociationProbabilities = [:]
    Map<String, Map<String, Double>> tagScores = [:]
    Map<String, Map<String, Double>> tagProbabilities = [:]

}
