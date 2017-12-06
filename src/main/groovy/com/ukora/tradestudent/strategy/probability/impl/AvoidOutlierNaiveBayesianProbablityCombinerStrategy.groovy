package com.ukora.tradestudent.strategy.probability.impl

import com.ukora.tradestudent.bayes.numbers.NumberAssociationProbability
import com.ukora.tradestudent.strategy.probability.ProbabilityCombinerStrategy
import com.ukora.tradestudent.utils.Logger
import org.springframework.stereotype.Component

import static com.ukora.tradestudent.utils.NerdUtils.assertRanges

@Component
class AvoidOutlierNaiveBayesianProbablityCombinerStrategy implements ProbabilityCombinerStrategy {

    public final static long MAX_RELEVANCE_MULTIPLE = 2

    @Override
    Double combineProbabilities(String tag, Map<String, Map<String, NumberAssociationProbability>> numberAssociationProbabilities) {
        Double tagAssociationProduct = 1d
        Double generalAssociationProduct = 1d
        numberAssociationProbabilities.each {
            Double tagProbability = it.value.get(tag)?.probability
            Double oppositeTagProbability = -tagProbability
            Double relevance = Math.abs(it.value.get(tag)?.relevance)
            if((MAX_RELEVANCE_MULTIPLE * relevance) < Math.abs(tagProbability)) {
                Logger.debug("throwing out " + it.key + " p:" + tagProbability + " too far from r:" + relevance)
                return
            }
            if(!assertRanges(tagProbability, oppositeTagProbability, relevance)) {
                return
            }
            tagProbability = (tagProbability + 1) / 2
            oppositeTagProbability = (oppositeTagProbability + 1) / 2
            tagAssociationProduct = tagAssociationProduct * tagProbability
            generalAssociationProduct = generalAssociationProduct * oppositeTagProbability
        }
        return (0.5 * tagAssociationProduct) / generalAssociationProduct
    }

}
