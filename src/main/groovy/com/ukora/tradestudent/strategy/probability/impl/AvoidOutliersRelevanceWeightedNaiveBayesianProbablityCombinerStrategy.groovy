package com.ukora.tradestudent.strategy.probability.impl

import com.ukora.tradestudent.bayes.numbers.NumberAssociationProbability
import com.ukora.tradestudent.strategy.probability.ProbabilityCombinerStrategy
import com.ukora.tradestudent.utils.Logger
import org.springframework.stereotype.Component

import static com.ukora.tradestudent.utils.NerdUtils.assertRanges

@Component
class AvoidOutliersRelevanceWeightedNaiveBayesianProbablityCombinerStrategy implements ProbabilityCombinerStrategy {

    public final static long MIN_RELEVANCE = 0.0025

    boolean enabled = true

    @Override
    boolean isEnabled() {
        return enabled
    }

    @Override
    Double combineProbabilities(String tag, Map<String, Map<String, NumberAssociationProbability>> numberAssociationProbabilities) {
        Double tagAssociationProduct = 1d
        Double generalAssociationProduct = 1d
        Double tagProbability
        Double oppositeTagProbability
        Double relevance
        numberAssociationProbabilities.each {
            tagProbability = it.value.get(tag)?.probability
            oppositeTagProbability = -tagProbability
            relevance = Math.abs(it.value.get(tag)?.relevance)
            if(MIN_RELEVANCE < relevance) {
                Logger.debug("throwing out " + it.key + " r:" + relevance + " too far from min:" + MIN_RELEVANCE)
                return
            }
            if(!assertRanges(tagProbability, oppositeTagProbability, relevance)) {
                return
            }
            tagAssociationProduct = tagAssociationProduct * (((relevance * tagProbability) + 1) / 2)
            generalAssociationProduct = generalAssociationProduct * (((relevance * oppositeTagProbability) + 1) / 2)
        }
        return (0.5 * tagAssociationProduct) / generalAssociationProduct
    }

}
