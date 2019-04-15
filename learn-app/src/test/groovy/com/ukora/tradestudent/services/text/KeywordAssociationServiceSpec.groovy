package com.ukora.tradestudent.services.text

import spock.lang.Specification
import spock.lang.Unroll

class KeywordAssociationServiceSpec extends Specification {

    KeywordAssociationService keywordAssociationService = new KeywordAssociationService()

    @Unroll
    def "test extractProbability"() {

        when:
        def r = keywordAssociationService.extractProbability(tagProportion, counterTagProportion, tagKeywordAssociationCount, counterTagKeywordAssociationCount)

        then:
        r == expected

        where:
        tagProportion     | counterTagProportion | tagKeywordAssociationCount | counterTagKeywordAssociationCount | expected
        0.538461538461538 | 0.461538461538462    | 40                         | 30                                | 0.5217391304347833

    }

}
