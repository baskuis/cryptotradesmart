package com.ukora.tradestudent.services

import com.ukora.tradestudent.bayes.numbers.NumberAssociation
import com.ukora.tradestudent.bayes.numbers.NumberAssociationProbability
import com.ukora.tradestudent.entities.BrainNode
import com.ukora.tradestudent.entities.CorrelationAssociation
import com.ukora.tradestudent.strategy.probability.ProbabilityCombinerStrategy
import com.ukora.tradestudent.tags.AbstractCorrelationTag
import com.ukora.tradestudent.tags.TagGroup
import com.ukora.tradestudent.tags.TagSubset
import com.ukora.tradestudent.utils.Logger
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

    Map<String, TagGroup> tagGroupMap = [:]
    Map<String, AbstractCorrelationTag> tagMap = [:]

    @PostConstruct
    void init(){

        /** Collect primary tags */
        applicationContext.getBeansOfType(AbstractCorrelationTag).each {
            Logger.log(String.format("Found tag %s", it.key))
            tagMap.put(it.key, it.value)
        }

        /** Collect tag groups */
        applicationContext.getBeansOfType(TagGroup).each {
            Logger.log(String.format("Found tag group %s", it.key))
            tagGroupMap.put(it.key, it.value)
        }

    }

    /**
     * Get correlation associations
     *
     * @param eventDate
     * @return
     */
    CorrelationAssociation getCorrelationAssociations(Date eventDate){

        Logger.log(String.format('attempting to get association for %s', eventDate))
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
            tagGroupMap.each {
                NumberAssociation generalAssociation = brainNode.tagReference.get(CaptureAssociationsService.GENERAL + CaptureAssociationsService.SEP + it.value.name)
                if(generalAssociation) {
                    it.value.tags().each {
                        String tag = it.getTagName()
                        NumberAssociation tagAssociation = brainNode.tagReference.get(tag)
                        if(tagAssociation) {
                            tagAssociation.relevance = NerdUtils.chanceOfCorrelation(
                                    tagAssociation.mean,
                                    tagAssociation.standard_deviation,
                                    tagAssociation.mean,
                                    generalAssociation.standard_deviation,
                                    generalAssociation.mean
                            )
                        }
                    }
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
            tagGroupMap.each {
                NumberAssociation generalAssociation = brainNode.tagReference.get(CaptureAssociationsService.GENERAL + CaptureAssociationsService.SEP + it.value.name)
                if(generalAssociation) {
                    it.value.tags().each {
                        String tag = it.getTagName()
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
    }

    /**
     * Hydrate tag scores
     *
     * @param correlationAssociation
     */
    void hydrateTagScores(CorrelationAssociation correlationAssociation){
        tagGroupMap.each {
            it.value.tags().each {
                String tag = it.getTagName()
                Map<String, ProbabilityCombinerStrategy> probabilityCombinerStrategyMap = applicationContext.getBeansOfType(ProbabilityCombinerStrategy)
                probabilityCombinerStrategyMap.each {
                    if (it.value instanceof TagSubset && !(it.value as TagSubset).applies(tag)) {
                        Logger.debug(String.format("Skipping p combiner strategy: %s for tag: %s", it.key, tag))
                        return
                    }
                    Logger.debug(String.format("Running p combiner strategy: %s", it.key))
                    correlationAssociation.tagScores.get(tag, [:]).put(it.key, it.value.combineProbabilities(tag, correlationAssociation.numericAssociationProbabilities))
                }
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
                Double proportion = NerdUtils.getProportionOf(focusTag, tagGroupMap.find { it.value.applies(tag) }.value.tags().findAll({ it.getTagName() != tag }).collect({ correlationAssociation.tagScores.get(it.getTagName()).get(strategy) }))
                correlationAssociation.tagProbabilities.get(strategy, [:]).put(tag, proportion)
            }
        }
    }

}
