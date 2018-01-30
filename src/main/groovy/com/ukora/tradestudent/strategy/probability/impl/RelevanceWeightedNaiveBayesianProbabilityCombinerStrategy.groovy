package com.ukora.tradestudent.strategy.probability.impl

import com.ukora.tradestudent.bayes.numbers.NumberAssociationProbability
import com.ukora.tradestudent.strategy.probability.ProbabilityCombinerStrategy
import org.springframework.stereotype.Component

import static com.ukora.tradestudent.utils.NerdUtils.assertRanges

@Component
class RelevanceWeightedNaiveBayesianProbabilityCombinerStrategy implements ProbabilityCombinerStrategy {

    boolean enabled = true

    @Override
    boolean isEnabled() {
        return enabled
    }

    @Override
    String getAlias() {
        return "dave"
    }

    /**
     * This naive bayesian comparison calculates the tag vs baseline score (not probability)
     *
     *      Ptag * (Ra1 * Ptaga1) * (Ra2 * Ptaga2)
     * P = ----------------------------------------
     *            (Ra1 * Pa1) * (Ra2 * Pa2)
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
            tagProbability = (1 + it.value.get(tag)?.probability) / 2
            oppositeTagProbability = 1 - tagProbability
            componentWeightFactor = Math.abs(it.value.get(tag)?.relevance)
            if(assertRanges(tagProbability, oppositeTagProbability, componentWeightFactor)){
                tagProbability = ((componentWeightFactor * tagProbability) + 1) / 2
                oppositeTagProbability = ((componentWeightFactor * oppositeTagProbability) + 1) / 2
                tagAssociationProduct = tagAssociationProduct * tagProbability
                generalAssociationProduct = generalAssociationProduct * oppositeTagProbability
            }
        }
        return (0.5 * tagAssociationProduct) / generalAssociationProduct
    }

}
