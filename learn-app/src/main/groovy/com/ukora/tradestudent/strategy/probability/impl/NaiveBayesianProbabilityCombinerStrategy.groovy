package com.ukora.tradestudent.strategy.probability.impl

import com.ukora.domain.beans.bayes.numbers.NumberAssociationProbability
import com.ukora.tradestudent.strategy.probability.ProbabilityCombinerStrategy
import org.springframework.stereotype.Component

import static com.ukora.tradestudent.utils.NerdUtils.assertRanges

@Component
class NaiveBayesianProbabilityCombinerStrategy implements ProbabilityCombinerStrategy {

    boolean enabled = true

    @Override
    boolean isEnabled() {
        return enabled
    }

    @Override
    String getAlias() {
        return "james"
    }

    /**
     * This naive bayesian comparison calculates the tag vs baseline score (not probability)
     *
     *      Ptag * Ptaga1 * Ptaga2
     * P = ------------------------
     *            Pa1 * Pa2
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
        numberAssociationProbabilities.each {
            tagProbability = it.value.get(tag)?.probability
            if(!tagProbability) return
            oppositeTagProbability = -tagProbability
            if(assertRanges(tagProbability, oppositeTagProbability)){
                tagProbability = (tagProbability + 1) / 2
                oppositeTagProbability = (oppositeTagProbability + 1) / 2
                tagAssociationProduct = tagAssociationProduct * tagProbability
                generalAssociationProduct = generalAssociationProduct * oppositeTagProbability
            }
        }
        return ((1 / 2) * tagAssociationProduct) / generalAssociationProduct
    }

}
