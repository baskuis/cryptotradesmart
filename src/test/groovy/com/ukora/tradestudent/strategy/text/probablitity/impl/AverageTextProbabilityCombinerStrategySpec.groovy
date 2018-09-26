package com.ukora.tradestudent.strategy.text.probablitity.impl

import com.ukora.tradestudent.entities.ExtractedText
import com.ukora.tradestudent.entities.KeywordAssociation
import spock.lang.Specification
import spock.lang.Unroll

class AverageTextProbabilityCombinerStrategySpec extends Specification {

    AverageTextProbabilityCombinerStrategy probabilityCombinerStrategy

    def setup() {
        probabilityCombinerStrategy = new AverageTextProbabilityCombinerStrategy()
    }

    @Unroll
    def "test combineProbabilities #scenario with tag #tag produces #expected"() {

        setup:
        Map<String, KeywordAssociation> keywordAssociationProbabilities = [
                'foo' : new KeywordAssociation(
                        source: ExtractedText.TextSource.TWITTER,
                        tagProbabilities: [
                                'buy': 0.50,
                                'sell': 0.70
                        ]
                ),
                'bar': new KeywordAssociation(
                        source: ExtractedText.TextSource.TWITTER,
                        tagProbabilities: [
                                'buy': 0.60,
                                'sell': 0.80
                        ]
                )
        ]

        when:
        def r = probabilityCombinerStrategy.combineProbabilities(tag, keywordAssociationProbabilities)

        then:
        r == expected

        where:
        scenario  | tag   | expected
        'tag one' | 'buy' | 0.775
        'tag two' | 'sell' | 0.875

    }
}
