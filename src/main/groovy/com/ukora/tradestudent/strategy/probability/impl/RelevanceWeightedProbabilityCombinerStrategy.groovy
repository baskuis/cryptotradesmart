package com.ukora.tradestudent.strategy.probability.impl

import com.ukora.tradestudent.bayes.numbers.NumberAssociationProbability
import com.ukora.tradestudent.strategy.probability.ProbabilityCombinerStrategy
import org.springframework.stereotype.Component

@Component
class RelevanceWeightedProbabilityCombinerStrategy implements ProbabilityCombinerStrategy {

    boolean enabled = true

    @Override
    boolean isEnabled() {
        return enabled
    }

    /**
     *      R1*P1 + R2*P2
     * P = ---------------
     *         R1 + R2
     *
     * @param tag
     * @param numberAssociationProbabilities
     * @return
     */
    @Override
    Double combineProbabilities(String tag, Map<String, Map<String, NumberAssociationProbability>> numberAssociationProbabilities) {
        Double totalRelevance = 0
        Double toplineRelevanceProbability = 0
        Double relevance
        Double probability
        numberAssociationProbabilities.each {
            relevance = it.value.get(tag).relevance
            probability = (1 + it.value.get(tag).probability) / 2
            if(!relevance || Double.isNaN(relevance)) return
            if(!probability || Double.isNaN(probability)) return
            toplineRelevanceProbability += Math.abs(relevance) * probability
            totalRelevance += Math.abs(relevance)
        }
        return toplineRelevanceProbability / totalRelevance
    }
    
}
