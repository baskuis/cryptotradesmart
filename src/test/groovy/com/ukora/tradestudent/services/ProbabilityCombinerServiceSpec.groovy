package com.ukora.tradestudent.services

import com.ukora.tradestudent.bayes.numbers.NumberAssociation
import com.ukora.tradestudent.entities.BrainNode
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

    BytesFetcherService bytesFetcherService = Mock(BytesFetcherService)

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
    
    def "test getBrainNodes"() {

        setup:
        probabilityCombinerService.applicationContext = applicationContext
        probabilityCombinerService.bytesFetcherService = bytesFetcherService

        when:
        probabilityCombinerService.init()
        Map<String, BrainNode> nodes = probabilityCombinerService.getBrainNodes()

        then:
        1 * applicationContext.getBeansOfType(TagGroup) >> [
                'buySellTagGroup': new BuySellTagGroup(buyTag: new BuyTag(), sellTag: new SellTag()),
                'upDownTagGroup': new UpDownTagGroup(upTag: new UpTag(), downTag: new DownTag())
        ]
        1 * bytesFetcherService.getAllBrainNodes() >> ['low.relevance.example':
                new BrainNode(
                        tagReference: [
                                'buy': new NumberAssociation(
                                        count: 10,
                                        relevance: null,
                                        mean: 0.4,
                                        standard_deviation: 0.05
                                ),
                                'sell': new NumberAssociation(
                                        count: 10,
                                        relevance: null,
                                        mean: 0.5,
                                        standard_deviation: 0.05
                                ),
                                'up': new NumberAssociation(
                                        count: 10,
                                        relevance: 0.1,
                                        mean: 0.6,
                                        standard_deviation: 0.06
                                ),
                                'down': new NumberAssociation(
                                        count: 10,
                                        relevance: 0.1,
                                        mean: 0.5,
                                        standard_deviation: 0.06
                                )

                        ]
                ), 'high.relevance.example': new BrainNode(
                        tagReference: [
                                'buy': new NumberAssociation(
                                        count: 10,
                                        relevance: null,
                                        mean: 0.4,
                                        standard_deviation: 0.02
                                ),
                                'sell': new NumberAssociation(
                                        count: 10,
                                        relevance: null,
                                        mean: 0.5,
                                        standard_deviation: 0.02
                                ),
                                'up': new NumberAssociation(
                                        count: 10,
                                        relevance: 0.1,
                                        mean: 0.6,
                                        standard_deviation: 0.03
                                ),
                                'down': new NumberAssociation(
                                        count: 10,
                                        relevance: 0.1,
                                        mean: 0.5,
                                        standard_deviation: 0.03
                                )

                        ]
                ), 'medium.relevance.example':
                new BrainNode(
                        tagReference: [
                                'buy': new NumberAssociation(
                                        count: 10,
                                        relevance: null,
                                        mean: 0.4,
                                        standard_deviation: 0.04
                                ),
                                'sell': new NumberAssociation(
                                        count: 10,
                                        relevance: null,
                                        mean: 0.5,
                                        standard_deviation: 0.04
                                ),
                                'up': new NumberAssociation(
                                        count: 10,
                                        relevance: 0.1,
                                        mean: 0.6,
                                        standard_deviation: 0.05
                                ),
                                'down': new NumberAssociation(
                                        count: 10,
                                        relevance: 0.1,
                                        mean: 0.5,
                                        standard_deviation: 0.05
                                )

                        ]
                )
        ]

        expect:
        nodes.values()[0].tagReference.get('buy').relevance == 0.9149457826670966
        nodes.values()[1].tagReference.get('buy').relevance == 0.37138805229168415
        nodes.values()[2].tagReference.get('buy').relevance == 0.24453711745832485

    }

}
