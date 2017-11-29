package com.ukora.tradestudent.strategy.impl

import com.ukora.tradestudent.bayes.numbers.NumberAssociationProbability
import com.ukora.tradestudent.strategy.ProbabilityCombinerStrategy
import org.springframework.stereotype.Component

@Component
class NaiveBayesianProbabilityCobinerStrategy implements ProbabilityCombinerStrategy {

    /** TODO: Bayes naive algorithm needed for consideration */
    @Override
    Double combineProbabilities(String tag, Map<String, Map<String, NumberAssociationProbability>> numberAssociationProbabilities) {
        return null
    }

}
