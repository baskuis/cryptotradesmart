package com.ukora.tradestudent.strategy.text.probablitity.impl

import com.ukora.tradestudent.entities.KeywordAssociation
import com.ukora.tradestudent.strategy.text.probablitity.TextProbabilityCombinerStrategy
import org.springframework.stereotype.Component

@Component
class AvoidOutliersWeightedTextProbabilityCombinerStrategy implements TextProbabilityCombinerStrategy {

    static Double MIN_DEVIANCE = 0.05

    @Override
    String getAlias() {
        return 'binckley'
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
            if (!value) return
            if (MIN_DEVIANCE > Math.abs(value - 0.5)) return
            weight = Math.abs(value - 0.5)
            probability = value
            topLineProbability += weight * probability
            totalProbability += weight
        }
        return topLineProbability / totalProbability
    }

}