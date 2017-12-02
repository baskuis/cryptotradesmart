package com.ukora.tradestudent.strategy.impl

import com.ukora.tradestudent.bayes.numbers.NumberAssociationProbability
import com.ukora.tradestudent.strategy.ProbabilityCombinerStrategy
import org.springframework.stereotype.Component

@Component
class BuySellNaiveBayesianProbabilityCombinerStrategy implements ProbabilityCombinerStrategy {

    private static final String SELL_TAG_NAME = 'sell'
    private static final String BUY_TAG_NAME = 'buy'

    /**
     * This naive bayesian comparison calculates the buy vs sell probability
     *
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
        numberAssociationProbabilities.each {
            Double tagProbability = it.value.get(tag)?.probability
            Double oppositeTagProbability = it.value.get(tag == SELL_TAG_NAME ? BUY_TAG_NAME : SELL_TAG_NAME)?.probability
            if(tagProbability > 0 && !tagProbability.naN && oppositeTagProbability > 0 && !oppositeTagProbability.naN){
                tagProbability = (tagProbability + 1) / 2
                oppositeTagProbability = (oppositeTagProbability + 1) / 2
                tagAssociationProduct = tagAssociationProduct * tagProbability
                generalAssociationProduct = generalAssociationProduct * oppositeTagProbability
            }
        }
        return ((1 / 2) * tagAssociationProduct) / generalAssociationProduct
    }

}
