package com.ukora.tradestudent.strategy.impl

import com.ukora.tradestudent.bayes.numbers.NumberAssociationProbability
import com.ukora.tradestudent.strategy.ProbabilityCombinerStrategy
import org.springframework.stereotype.Component

@Component
class RelevanceWeightedProbabilityCombinerStrategy implements ProbabilityCombinerStrategy {

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
        numberAssociationProbabilities.each {
            Double relevance = it.value.get(tag).relevance
            Double probability = it.value.get(tag).probability
            if(!relevance || Double.isNaN(relevance)) return
            if(!probability || Double.isNaN(probability)) return
            toplineRelevanceProbability += Math.abs(relevance) * probability
            totalRelevance += Math.abs(relevance)
        }
        return toplineRelevanceProbability / totalRelevance
    }
    
}
