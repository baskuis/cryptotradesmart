package com.ukora.tradestudent.services

import com.ukora.tradestudent.entities.CorrelationAssociation
import com.ukora.tradestudent.tags.TagGroup
import com.ukora.tradestudent.tags.buysell.BuySellTagGroup
import com.ukora.tradestudent.tags.buysell.BuyTag
import com.ukora.tradestudent.tags.buysell.SellTag
import com.ukora.tradestudent.tags.trend.DownTag
import com.ukora.tradestudent.tags.trend.UpDownTagGroup
import com.ukora.tradestudent.tags.trend.UpTag
import org.springframework.context.ApplicationContext
import spock.lang.Specification

class ProbabilityCombinerServiceSpec extends Specification {

    ProbabilityCombinerService probabilityCombinerService = new ProbabilityCombinerService()

    ApplicationContext applicationContext = Mock(ApplicationContext)

    /**
     * This test makes sure that tag groups are correctly honored
     * so that proportional scores are compared to tags within that
     * tag group only
     *
     * @return
     */
    def "test hydrateTagProbabilities"() {

        setup:
        String pCombiner = 'strategy1'
        probabilityCombinerService.applicationContext = applicationContext
        CorrelationAssociation correlationAssociation = new CorrelationAssociation()
        correlationAssociation.tagScores = [
                'buy' : [(pCombiner): 2.5d],
                'sell' : [(pCombiner): 0.5d],
                'up' : [(pCombiner): 0.5d],
                'down' : [(pCombiner): 3.5d]
        ]

        when:
        probabilityCombinerService.init()
        probabilityCombinerService.hydrateTagProbabilities(correlationAssociation)

        then:
        1 * applicationContext.getBeansOfType(TagGroup) >> [
                'buySellTagGroup': new BuySellTagGroup(buyTag: new BuyTag(), sellTag: new SellTag()),
                'upDownTagGroup': new UpDownTagGroup(upTag: new UpTag(), downTag: new DownTag())
        ]
        correlationAssociation.tagProbabilities != null
        correlationAssociation.tagProbabilities[pCombiner]['buy'] == 0.8333333333333334d
        correlationAssociation.tagProbabilities[pCombiner]['sell'] == 0.16666666666666666d
        correlationAssociation.tagProbabilities[pCombiner]['up'] == 0.125d
        correlationAssociation.tagProbabilities[pCombiner]['down'] == 0.875d

    }

}
