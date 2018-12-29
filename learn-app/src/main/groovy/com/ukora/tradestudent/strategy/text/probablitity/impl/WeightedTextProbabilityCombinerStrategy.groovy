package com.ukora.tradestudent.strategy.text.probablitity.impl

import com.ukora.domain.entities.KeywordAssociation
import com.ukora.tradestudent.strategy.text.probablitity.TextProbabilityCombinerStrategy
import org.springframework.stereotype.Component

@Component
class WeightedTextProbabilityCombinerStrategy implements TextProbabilityCombinerStrategy {

    @Override
    String getAlias() {
        return 'shula'
    }

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
    Double combineProbabilities(String tag, Map<String, KeywordAssociation> keywordAssociationProbabilities) {
        Double topLineProbability = 0
        Double totalProbability = 0
        Double probability
        Double weight
        Double value
        keywordAssociationProbabilities.each {
            value = it.value.tagProbabilities.get(tag)
            if(!value) return
            weight = Math.abs(value - 0.5)
            probability = value
            topLineProbability += weight * probability
            totalProbability += weight
        }
        return topLineProbability / totalProbability
    }

}
