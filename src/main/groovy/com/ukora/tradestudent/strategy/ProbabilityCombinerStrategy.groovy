package com.ukora.tradestudent.strategy

import com.ukora.tradestudent.bayes.numbers.NumberAssociationProbability

interface ProbabilityCombinerStrategy {

    Double combineProbabilities(String tag, Map<String, Map<String, NumberAssociationProbability>> numberAssociationProbabilities)

}