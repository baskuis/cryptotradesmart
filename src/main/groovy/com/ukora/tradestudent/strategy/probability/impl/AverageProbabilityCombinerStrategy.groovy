package com.ukora.tradestudent.strategy.probability.impl

import com.ukora.tradestudent.bayes.numbers.NumberAssociationProbability
import com.ukora.tradestudent.strategy.probability.ProbabilityCombinerStrategy
import org.springframework.stereotype.Component

@Component
class AverageProbabilityCombinerStrategy implements ProbabilityCombinerStrategy {

    boolean enabled = true

    @Override
    boolean isEnabled() {
        return enabled
    }

    @Override
    Double combineProbabilities(String tag, Map<String, Map<String, NumberAssociationProbability>> numberAssociationProbabilities) {
        Double total = 0
        numberAssociationProbabilities.each {
            total += it.value.get(tag).probability
        }
        return total / numberAssociationProbabilities.size()
    }

}
