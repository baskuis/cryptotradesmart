package com.ukora.domain.entities

import com.ukora.domain.beans.bayes.numbers.NumberAssociationProbability
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient

import java.util.concurrent.ConcurrentHashMap

class CorrelationAssociation extends AbstractAssociation {

    @Id
    String id

    Map<String, Double> numericAssociations = new ConcurrentHashMap<>()

    @Transient
    Map<String, Map<String, NumberAssociationProbability>> numericAssociationProbabilities = new ConcurrentHashMap<>()

    Map<String, Map<String, Double>> tagScores = new ConcurrentHashMap<>()
    Map<String, Map<String, Double>> tagProbabilities = new ConcurrentHashMap<>()

}
