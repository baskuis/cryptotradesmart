package com.ukora.tradestudent.services

import com.ukora.tradestudent.bayes.numbers.NumberAssociation
import com.ukora.tradestudent.bayes.numbers.NumberAssociationProbability
import com.ukora.tradestudent.entities.BrainNode
import com.ukora.tradestudent.entities.CorrelationAssociation
import com.ukora.tradestudent.strategy.probability.ProbabilityCombinerStrategy
import com.ukora.tradestudent.tags.AbstractCorrelationTag
import com.ukora.tradestudent.tags.TagSubset
import com.ukora.tradestudent.utils.Logger
import com.ukora.tradestudent.utils.NerdUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct

@Service
class ProbabilityFigurerService {

    //TODO: Test if this applies or is useful
    public final static Double MIN_RELEVANCE = 0.01

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

        /** calculate combined tag correlation scores */
        hydrateTagScores(correlationAssociation)

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
            if(genericAssociation) {
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
            String reference = it.key
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
                        correlationAssociation.numericAssociationProbabilities.get(reference, [:]).put(tag, numberAssociationProbability)
                    }
                }
            }
        }
    }

    /**
     * Hydrate tag scores
     *
     * @param correlationAssociation
     */
    void hydrateTagScores(CorrelationAssociation correlationAssociation){
        primaryTags.each { String tag ->
            Map<String, ProbabilityCombinerStrategy> probabilityCombinerStrategyMap = applicationContext.getBeansOfType(ProbabilityCombinerStrategy)
            probabilityCombinerStrategyMap.each {
                if(it.value instanceof TagSubset && !(it.value as TagSubset).applies(tag)){
                    Logger.debug(String.format("Skipping p combiner strategy: %s for tag: %s", it.key, tag))
                    return
                }
                Logger.debug(String.format("Running p combiner strategy: %s", it.key))
                correlationAssociation.tagScores.get(tag, [:]).put(it.key, it.value.combineProbabilities(tag, correlationAssociation.numericAssociationProbabilities))
            }
        }
    }

    /**
     * Hydrate tag probabilities
     *
     * @param correlationAssociation
     */
    void hydrateTagProbabilities(CorrelationAssociation correlationAssociation){
        correlationAssociation.tagScores.each {
            String tag = it.key
            it.value.each {
                String strategy = it.key
                Double focusTag = correlationAssociation.tagScores.get(tag).get(strategy)
                Double proportion = NerdUtils.getProportionOf(focusTag, primaryTags.findAll { it != tag }.collect { correlationAssociation.tagScores.get(it).get(strategy) })
                correlationAssociation.tagProbabilities.get(strategy, [:]).put(tag, proportion)
            }
        }
    }

}
