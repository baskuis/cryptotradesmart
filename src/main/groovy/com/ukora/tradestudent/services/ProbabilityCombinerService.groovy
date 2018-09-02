package com.ukora.tradestudent.services

import com.ukora.tradestudent.bayes.numbers.NumberAssociation
import com.ukora.tradestudent.bayes.numbers.NumberAssociationProbability
import com.ukora.tradestudent.entities.BrainNode
import com.ukora.tradestudent.entities.CorrelationAssociation
import com.ukora.tradestudent.services.associations.CaptureAssociationsService
import com.ukora.tradestudent.strategy.probability.ProbabilityCombinerStrategy
import com.ukora.tradestudent.tags.AbstractCorrelationTag
import com.ukora.tradestudent.tags.TagGroup
import com.ukora.tradestudent.tags.TagSubset
import com.ukora.tradestudent.utils.Logger
import com.ukora.tradestudent.utils.NerdUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import java.util.concurrent.ConcurrentHashMap

@Service
class ProbabilityCombinerService {

    @Autowired
    CaptureAssociationsService captureAssociationsService

    @Autowired
    BytesFetcherService bytesFetcherService

    @Autowired
    ApplicationContext applicationContext

    Map<String, TagGroup> tagGroupMap = [:]
    Map<String, AbstractCorrelationTag> tagMap = [:]

    static public boolean multiThreadingEnabled = true

    public final static int numCores = Runtime.getRuntime().availableProcessors()

    /**
     * This is a factor by which the bell-curve proportion will be softened
     * and will determine at which point the threshold will cause P=0 with outliers
     * vs p -1 or p 1
     *
     */
    final static Double SOFTENING_FACTOR = 0.01

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
    @Cacheable("associations")
    CorrelationAssociation getCorrelationAssociations(Date eventDate){

        Logger.log(String.format('attempting to get association for %s', eventDate))
        CorrelationAssociation correlationAssociation = new CorrelationAssociation()
        correlationAssociation.setDate(eventDate)

        /** hydrate correlative data from memory */
        bytesFetcherService.hydrateAssociation(correlationAssociation)

        /** hydrate associations */
        captureAssociationsService.hydrateAssociations(correlationAssociation)

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
    @Cacheable("brainNodes")
    Map<String, BrainNode> getBrainNodes(){
        Map<String, BrainNode> nodes = bytesFetcherService.getAllBrainNodes()
        nodes.each {
            BrainNode brainNode = it.value
            tagGroupMap.each {
                TagGroup tagGroup = it.value
                NumberAssociation generalAssociation = TagService.getGeneralNumberAssociation(brainNode, tagGroup)
                brainNode.tagReference.put(generalAssociation.tag, generalAssociation)
                if(generalAssociation) {
                    it.value.tags().each {
                        String tag = it.getTagName()
                        NumberAssociation tagAssociation = brainNode.tagReference.get(tag)
                        if(tagAssociation) {
                            tagAssociation.relevance = NerdUtils.chanceOfCorrelationSoftening(
                                    tagAssociation.mean,
                                    tagAssociation.standard_deviation,
                                    tagAssociation.mean,
                                    tagAssociation.standard_deviation,
                                    generalAssociation.mean,
                                    SOFTENING_FACTOR
                            )
                        }
                    }
                }
            }
        }
        return nodes?.sort({
            def tags = it.getValue().tagReference.findAll({
                !it.key.contains('general')
            })
            return -((tags?.values()?.sum({
                it.relevance?:0
            })?:0) / (tags?.values()?.size()?:1))
        })
    }

    /**
     * Hydrate probabilities
     * comparing each tag to the 'general' collection for
     * this numeric association
     *
     * @param correlationAssociation
     */
    void hydrateProbabilities(CorrelationAssociation correlationAssociation){
        def partitioned = multiThreadingEnabled ? NerdUtils.partitionMap(correlationAssociation.numericAssociations, numCores) : [correlationAssociation.numericAssociations]
        if (multiThreadingEnabled) Logger.debug(String.format("Delegating simulation to %s threads", numCores))
        if (multiThreadingEnabled) Logger.debug(String.format("Split up simulations into %s groups", partitioned?.size()))
        Map<String, BrainNode> brainNodes = getBrainNodes()
        List<Thread> threads = []
        partitioned.collect { Map<String, Double> group ->
            threads << Thread.start({
                group.each {
                    String reference = it.key
                    BrainNode brainNode = brainNodes.get(it.key)
                    Double normalizedValue = it.value
                    tagGroupMap.each {
                        TagGroup tagGroup = it.value
                        if(brainNode) {
                            NumberAssociation generalAssociation = brainNode.tagReference.get(CaptureAssociationsService.GENERAL + CaptureAssociationsService.SEP + tagGroup.name)
                            if (generalAssociation) {
                                it.value.tags().each {
                                    String tag = it.getTagName()
                                    NumberAssociation tagAssociation = brainNode.tagReference.get(tag)
                                    if (tagAssociation) {
                                        NumberAssociationProbability numberAssociationProbability = new NumberAssociationProbability(tagAssociation)
                                        numberAssociationProbability.probability = NerdUtils.chanceOfCorrelationSoftening(
                                                normalizedValue,
                                                tagAssociation.standard_deviation,
                                                tagAssociation.mean,
                                                tagAssociation.standard_deviation,
                                                generalAssociation.mean,
                                                SOFTENING_FACTOR
                                        )
                                        correlationAssociation.numericAssociationProbabilities.get(reference, new ConcurrentHashMap<String, NumberAssociationProbability>()).put(tag, numberAssociationProbability)
                                    }
                                }
                            }
                        }
                    }
                }
            })
        }
        threads*.join()
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
                probabilityCombinerStrategyMap.findAll { it.value.enabled }.each {
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
