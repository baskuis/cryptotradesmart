package com.ukora.tradestudent.strategy.probability.impl

import com.ukora.tradestudent.bayes.numbers.NumberAssociationProbability
import com.ukora.tradestudent.strategy.probability.ProbabilityCombinerStrategy
import org.springframework.stereotype.Component

@Component
class WeightedProbalityCombinerStrategy implements ProbabilityCombinerStrategy {

    boolean enabled = true

    @Override
    boolean isEnabled() {
        return enabled
    }

    /**
     *      P1^2 + P2^2
     * P = ---------------
     *        P1 + P2
     *
     * @param tag
     * @param numberAssociationProbabilities
     * @return
     */
    @Override
    Double combineProbabilities(String tag, Map<String, Map<String, NumberAssociationProbability>> numberAssociationProbabilities) {
        Double toplineProbability = 0
        Double totalProbability = 0
        Double probability
        numberAssociationProbabilities.each {
            probability = it.value.get(tag).probability
            if(!probability || Double.isNaN(probability)) return
            int multiplier = (probability < 0) ? -1 : 1
            toplineProbability += multiplier * Math.pow(probability, 2)
            totalProbability += Math.abs(probability)
        }
        return toplineProbability / totalProbability
    }

}