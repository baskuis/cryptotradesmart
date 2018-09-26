package com.ukora.tradestudent.strategy.text.probablitity.impl

import com.ukora.tradestudent.entities.ExtractedText
import com.ukora.tradestudent.entities.KeywordAssociation
import spock.lang.Specification
import spock.lang.Unroll

class AvoidOutliersWeightedTextProbabilityCombinerStrategySpec extends Specification {

    AvoidOutliersWeightedTextProbabilityCombinerStrategy probabilityCombinerStrategy

    def setup() {
        probabilityCombinerStrategy = new AvoidOutliersWeightedTextProbabilityCombinerStrategy()
    }

    @Unroll
    def "test combineProbabilities #scenario with tag #tag produces #expected"() {

        setup:
        Map<String, KeywordAssociation> keywordAssociationProbabilities = [
                'minimal': new KeywordAssociation(
                        source: ExtractedText.TextSource.TWITTER,
                        tagProbabilities: [
                                'buy' : 0.504,
                                'sell': 0.70
                        ]
                ),
                'foo'    : new KeywordAssociation(
                        source: ExtractedText.TextSource.TWITTER,
                        tagProbabilities: [
                                'buy' : 0.50,
                                'sell': 0.70
                        ]
                ),
                'bar'    : new KeywordAssociation(
                        source: ExtractedText.TextSource.TWITTER,
                        tagProbabilities: [
                                'buy' : 0.60,
                                'sell': 0.80
                        ]
                )
        ]

        when:
        def r = probabilityCombinerStrategy.combineProbabilities(tag, keywordAssociationProbabilities)

        then:
        r == expected

        where:
        scenario       | tag    | expected
        'with outlier' | 'buy'  | 0.6
        'no outlier'   | 'sell' | 0.7428571428571429

    }

}
