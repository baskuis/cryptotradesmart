package com.ukora.tradestudent.services

import com.ukora.tradestudent.bayes.numbers.NumberAssociation
import com.ukora.tradestudent.bayes.numbers.NumberAssociationProbability
import com.ukora.tradestudent.entities.BrainNode
import com.ukora.tradestudent.entities.CorrelationAssociation
import com.ukora.tradestudent.tags.AbstractCorrelationTag
import com.ukora.tradestudent.utils.NerdUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct

@Service
class ProbabilityFigurerService {

    @Autowired
    CaptureAssociationsService captureAssociationsService

    @Autowired
    BytesFetcherService bytesFetcherService

    @Autowired
    ApplicationContext applicationContext

    List<String> primaryTags = []

    @PostConstruct
    void init(){

        /** Collect available primary tags as a list */
        applicationContext.getBeansOfType(AbstractCorrelationTag).each {
            primaryTags << it.value.tagName
        }

    }

    /**
     * Get correlation associations
     *
     * @param eventDate
     * @return
     */
    CorrelationAssociation getCorrelationAssociations(Date eventDate){

        println String.format('attempting to get association for %s', eventDate)
        CorrelationAssociation correlationAssociation = new CorrelationAssociation()
        correlationAssociation.setDate(eventDate)

        /** hydrate correlative data from memory */
        bytesFetcherService.hydrateAssociation(correlationAssociation)

        /** hydrate associations */
        captureAssociationsService.hydrateAssocations(correlationAssociation)

        /** hydrate probability reference for each numeric reference */
        hydrateProbabilities(correlationAssociation)

        /** calculate combined tag correlation probability */
        hydrateTagProbabilities(correlationAssociation)

        return correlationAssociation

    }

    /**
     * Get brain nodes
     * also calculate the relevance score
     * showing predictive quality of each reference
     *
     * @return
     */
    Map<String, BrainNode> getBrainNodes(){
        Map<String, BrainNode> nodes = bytesFetcherService.getAllBrainNodes()
        nodes.each {
            BrainNode brainNode = it.value
            NumberAssociation genericAssociation = brainNode.tagReference.get(CaptureAssociationsService.GENERAL_ASSOCIATION_REFERENCE)
            primaryTags.each { String tag ->
                NumberAssociation tagAssociation = brainNode.tagReference.get(tag)
                tagAssociation.relevance = NerdUtils.chanceOfCorrelation(
                    tagAssociation.mean,
                    tagAssociation.standard_deviation,
                    tagAssociation.mean,
                    genericAssociation.standard_deviation,
                    genericAssociation.mean
                )
            }
        }
        return nodes
    }

    /**
     * Hydrate probabilities
     * comparing each tag to the 'general' collection for
     * this numeric association
     *
     * @param correlationAssociation
     */
    void hydrateProbabilities(CorrelationAssociation correlationAssociation){
        Map<String, BrainNode> brainNodes = getBrainNodes()
        correlationAssociation.numericAssociations.each {
            BrainNode brainNode = brainNodes.get(it.key)
            Double normalizedValue = it.value
            NumberAssociation generalAssociation = brainNode.tagReference.get(CaptureAssociationsService.GENERAL_ASSOCIATION_REFERENCE)
            if(generalAssociation) {
                primaryTags.each { String tag ->
                    NumberAssociation tagAssociation = brainNode.tagReference.get(tag)
                    if (tagAssociation) {
                        NumberAssociationProbability numberAssociationProbability = new NumberAssociationProbability(tagAssociation)
                        numberAssociationProbability.probability = NerdUtils.chanceOfCorrelation(
                                normalizedValue,
                                tagAssociation.standard_deviation,
                                tagAssociation.mean,
                                generalAssociation.standard_deviation,
                                tagAssociation.mean
                        )
                        correlationAssociation.numericAssociationProbabilities.get(it.key, [:]).put(it.key, numberAssociationProbability)
                    } else {
                        println "no tagAssociation"
                    }
                }
            } else {
                println "no generalAssociation cannot continue"
            }
        }
    }

    void hydrateTagProbabilities(CorrelationAssociation correlationAssociation){
        primaryTags.each { String tag ->
            correlationAssociation.tagProbabilities.put(tag, null)

            /**
             *
             * Sp (sum of probabilities)
             * R (relevance of metric)
             * Rm (max relevance)
             * P (probability)
             * Pmx (max probability)
             * Pmi (min probability)
             * Pf (final probability)
             *
             * Pf = R/Rm * ???
             *
             *
             */

        }
    }

}
