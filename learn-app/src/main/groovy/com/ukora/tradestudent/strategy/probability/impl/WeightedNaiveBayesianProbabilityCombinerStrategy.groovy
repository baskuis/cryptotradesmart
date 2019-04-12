package com.ukora.tradestudent.strategy.probability.impl

import com.ukora.domain.beans.bayes.numbers.NumberAssociationProbability
import com.ukora.tradestudent.strategy.probability.ProbabilityCombinerStrategy
import org.springframework.stereotype.Component

import static com.ukora.tradestudent.utils.NerdUtils.assertRanges

@Component
class WeightedNaiveBayesianProbabilityCombinerStrategy implements ProbabilityCombinerStrategy {

    boolean enabled = false

    @Override
    String getAlias() {
        return "anna"
    }

    @Override
    boolean isEnabled() {
        return enabled
    }

    /**
     * This naive bayesian comparison calculates the tag vs baseline score (not probability)
     *
     *      Ptag * (Wa1 * Ptaga1) * (Wa2 * Ptaga2)
     * P = ----------------------------------------
     *            (Wa1 * Pa1) * (Wa2 * Pa2)
     *
     * @param tag
     * @param numberAssociationProbabilities
     * @return
     */
    @Override
    Double combineProbabilities(String tag, Map<String, Map<String, NumberAssociationProbability>> numberAssociationProbabilities) {
        Double tagAssociationProduct = 1d
        Double generalAssociationProduct = 1d
        Double tagProbability
        Double oppositeTagProbability
        Double componentWeightFactor
        numberAssociationProbabilities.each {
            tagProbability = it.value.get(tag)?.probability
            if(!tagProbability) return
            oppositeTagProbability = -tagProbability
            componentWeightFactor = Math.abs(it.value.get(tag)?.probability)
            if(assertRanges(tagProbability, oppositeTagProbability, componentWeightFactor)){
                tagProbability = ((componentWeightFactor * tagProbability) + 1) / 2
                oppositeTagProbability = ((componentWeightFactor * oppositeTagProbability) + 1) / 2
                tagAssociationProduct = tagAssociationProduct * tagProbability
                generalAssociationProduct = generalAssociationProduct * oppositeTagProbability
            }
        }
        return ((1 / 2) * tagAssociationProduct) / generalAssociationProduct
    }

}
