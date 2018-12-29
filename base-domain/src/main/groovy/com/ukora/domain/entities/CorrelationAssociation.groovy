package com.ukora.domain.entities

import com.ukora.domain.beans.bayes.numbers.NumberAssociationProbability

import java.util.concurrent.ConcurrentHashMap

class CorrelationAssociation extends AbstractAssociation {

    Map<String, Double> numericAssociations = new ConcurrentHashMap<>()
    Map<String, Map<String, NumberAssociationProbability>> numericAssociationProbabilities = new ConcurrentHashMap<>()
    Map<String, Map<String, Double>> tagScores = new ConcurrentHashMap<>()
    Map<String, Map<String, Double>> tagProbabilities = new ConcurrentHashMap<>()

}
