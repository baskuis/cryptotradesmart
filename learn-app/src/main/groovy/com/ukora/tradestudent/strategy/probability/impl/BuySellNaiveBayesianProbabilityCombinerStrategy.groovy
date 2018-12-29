package com.ukora.tradestudent.strategy.probability.impl

import com.ukora.domain.beans.bayes.numbers.NumberAssociationProbability
import com.ukora.tradestudent.strategy.probability.ProbabilityCombinerStrategy
import com.ukora.domain.beans.tags.TagSubset
import com.ukora.domain.beans.tags.buysell.BuySellTagGroup
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import static com.ukora.tradestudent.utils.NerdUtils.assertRanges

@Component
class BuySellNaiveBayesianProbabilityCombinerStrategy implements ProbabilityCombinerStrategy, TagSubset {

    private static final String SELL_TAG_NAME = 'sell'
    private static final String BUY_TAG_NAME = 'buy'

    @Autowired
    BuySellTagGroup buySellTagGroup

    @Override
    String getAlias() {
        return "sharon"
    }

    @Override
    boolean applies(String toTag) {
        buySellTagGroup.applies(toTag)
    }

    boolean enabled = false

    @Override
    boolean isEnabled() {
        return enabled
    }

    /**
     * This naive bayesian comparison calculates the buy vs sell score (not probability)
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
        if(tag != SELL_TAG_NAME && tag != BUY_TAG_NAME) return null
        Double tagAssociationProduct = 1d
        Double generalAssociationProduct = 1d
        Double tagProbability
        Double oppositeTagProbability
        numberAssociationProbabilities.each {
            tagProbability = it.value.get(tag)?.probability
            oppositeTagProbability = it.value.get(tag == SELL_TAG_NAME ? BUY_TAG_NAME : SELL_TAG_NAME)?.probability
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
