package com.ukora.tradestudent.strategy.impl

import com.ukora.tradestudent.bayes.numbers.NumberAssociationProbability
import com.ukora.tradestudent.strategy.ProbabilityCombinerStrategy

class RelevanceWeightedProbabilityCombinerStrategy implements ProbabilityCombinerStrategy {

    /**
     *
     * Sp (sum of probabilities)
     * R (relevance of metric)
     * Rm (max relevance)
     * P (probability)
     * Pmx (max probability)
     * Pmi (min probability)
     * Pf (final probability)
     *
     * Pf = R/Rm * ???
     *
     *
     */


    @Override
    Double combineProbabilities(String tag, Map<String, Map<String, NumberAssociationProbability>> numberAssociationProbabilities) {
        return null
    }
    
}
