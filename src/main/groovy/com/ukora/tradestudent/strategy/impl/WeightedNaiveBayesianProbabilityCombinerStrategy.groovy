package com.ukora.tradestudent.strategy.impl

import com.ukora.tradestudent.bayes.numbers.NumberAssociationProbability
import com.ukora.tradestudent.strategy.ProbabilityCombinerStrategy
import org.springframework.stereotype.Component

import static com.ukora.tradestudent.utils.NerdUtils.assertRanges

@Component
class WeightedNaiveBayesianProbabilityCombinerStrategy implements ProbabilityCombinerStrategy {

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
        numberAssociationProbabilities.each {
            Double tagProbability = it.value.get(tag)?.probability
            Double oppositeTagProbability = -tagProbability
            Double componentWeightFactor = Math.abs(it.value.get(tag)?.probability)
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
