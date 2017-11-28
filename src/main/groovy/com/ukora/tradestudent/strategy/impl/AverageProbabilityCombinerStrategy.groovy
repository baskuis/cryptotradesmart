package com.ukora.tradestudent.strategy.impl

import com.ukora.tradestudent.bayes.numbers.NumberAssociationProbability
import com.ukora.tradestudent.strategy.ProbabilityCombinerStrategy
import org.springframework.stereotype.Component

@Component
class AverageProbabilityCombinerStrategy implements ProbabilityCombinerStrategy {

    @Override
    Double combineProbabilities(String tag, Map<String, Map<String, NumberAssociationProbability>> numberAssociationProbabilities) {
        Double total = 0
        numberAssociationProbabilities.each { total += it.value.get(tag).probability }
        return total / numberAssociationProbabilities.size()
    }

}
