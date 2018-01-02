package com.ukora.tradestudent.strategy.probability.impl

import com.ukora.tradestudent.bayes.numbers.NumberAssociationProbability
import com.ukora.tradestudent.strategy.probability.ProbabilityCombinerStrategy
import com.ukora.tradestudent.utils.NerdUtils
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
            if(!it.value.get(tag) || !NerdUtils.assertRange(it.value.get(tag).probability)) return
            total += (1 + it.value.get(tag).probability) / 2
        }
        return total / numberAssociationProbabilities.size()
    }

}
