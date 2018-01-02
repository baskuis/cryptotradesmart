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
        Double weight
        numberAssociationProbabilities.each {
            if(!it.value.get(tag).probability) return
            weight = Math.abs(it.value.get(tag).probability)
            probability = (1 + it.value.get(tag).probability) / 2
            toplineProbability += weight * probability
            totalProbability += weight
        }
        return toplineProbability / totalProbability
    }

}