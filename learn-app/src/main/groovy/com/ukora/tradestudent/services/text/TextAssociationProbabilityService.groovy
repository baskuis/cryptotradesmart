package com.ukora.tradestudent.services.text

import com.ukora.tradestudent.entities.BrainCount
import com.ukora.tradestudent.entities.ExtractedText
import com.ukora.tradestudent.entities.KeywordAssociation
import com.ukora.tradestudent.entities.TextAssociations
import com.ukora.tradestudent.services.BytesFetcherService
import com.ukora.tradestudent.services.TagService
import com.ukora.tradestudent.services.associations.text.CaptureTextAssociationsService
import com.ukora.tradestudent.services.associations.text.TextExtractorService
import com.ukora.tradestudent.strategy.text.probablitity.TextProbabilityCombinerStrategy
import com.ukora.tradestudent.tags.AbstractCorrelationTag
import com.ukora.tradestudent.tags.TagGroup
import com.ukora.tradestudent.utils.Logger
import com.ukora.tradestudent.utils.NerdUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils

import javax.annotation.PostConstruct

@Service
class TextAssociationProbabilityService {

    @Autowired
    BytesFetcherService bytesFetcherService

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    TextExtractorService textExtractorService

    @Autowired
    KeywordAssociationService keywordAssociationService

    Map<String, TagGroup> tagGroupMap = [:]
    Map<String, AbstractCorrelationTag> tagMap = [:]
    Map<String, Integer> tagCount = [:]
    Map<String, TextProbabilityCombinerStrategy> textProbabilityCombinerStrategyMap = [:]

    @PostConstruct
    void init() {

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

        /** Collect textProbabilityCombinerStrategies */
        applicationContext.getBeansOfType(TextProbabilityCombinerStrategy).each {
            Logger.log(String.format("Found textProbabilityCombinerStrategy %s", it.key))
            textProbabilityCombinerStrategyMap.put(it.key, it.value)
        }

        /** Refresh tag count */
        refresh()

    }

    @Scheduled(initialDelay = 300000l, fixedRate = 300000l)
    @CacheEvict(value = "textTagCorrelations", allEntries = true)
    void refresh() {
        tagMap.each {
            Integer c = bytesFetcherService.getLessonCount(it.value.tagName)
            if(c) tagCount.put(it.value.tagName, c)
        }
    }

    /**
     * Get tag corrFelations by text
     *
     * @param eventDate
     */
    @Cacheable("textTagCorrelations")
    def tagCorrelationByText(Date eventDate){

        Logger.log(String.format('Attempting to get text association for %s', eventDate))
        ExtractedText extractedText = textExtractorService.extractTextForDate(eventDate)

        Map<String, KeywordAssociation> keywordAssociationsNews = [:]
        extractedText.extract(ExtractedText.TextSource.NEWS).unique().each {
            if(StringUtils.isEmpty(it)) return
            keywordAssociationsNews.put(it, keywordAssociationService.getKeywordAssociation(it, ExtractedText.TextSource.NEWS))
        }

        Map<String, KeywordAssociation> keywordAssociationsTwitter = [:]
        extractedText.extract(ExtractedText.TextSource.TWITTER).each {
            if(StringUtils.isEmpty(it)) return
            keywordAssociationsTwitter.put(it, keywordAssociationService.getKeywordAssociation(it, ExtractedText.TextSource.TWITTER))
        }

        Map<String, Map<String, Map<String, Double>>> strategyProbabilities = [:]

        textProbabilityCombinerStrategyMap.each {
            String strategy = it.key
            TextProbabilityCombinerStrategy textProbabilityCombinerStrategy = it.value
            Map<String, Map<String, Double>> tagsMap = strategyProbabilities.getOrDefault(strategy, [:])
            tagMap.each {
                AbstractCorrelationTag tag = it.value
                if(!tagsMap[ExtractedText.TextSource.NEWS.name()]) tagsMap[ExtractedText.TextSource.NEWS.name()] = [:]
                if(!tagsMap[ExtractedText.TextSource.TWITTER.name()]) tagsMap[ExtractedText.TextSource.TWITTER.name()] = [:]
                tagsMap[ExtractedText.TextSource.NEWS.name()][tag.tagName] = textProbabilityCombinerStrategy.combineProbabilities(tag.tagName, keywordAssociationsNews)
                tagsMap[ExtractedText.TextSource.TWITTER.name()][tag.tagName] = textProbabilityCombinerStrategy.combineProbabilities(tag.tagName, keywordAssociationsTwitter)
            }
            strategyProbabilities.put(strategy, getProportionedProbabilities(tagsMap))
        }

        return strategyProbabilities
    }

    /**
     * Get proportioned probabilities
     *
     * @param tagScores
     * @return
     */
    Map<String, Map<String, Double>> getProportionedProbabilities(Map<String, Map<String, Double>> tagScores){
        Map<String, Map<String, Double>> adjustedProbabilities = [:]
        tagScores.each {
            String source = it.key
            it.value.each {
                String tag = it.key
                Double score = it.value
                if(!adjustedProbabilities[source]) adjustedProbabilities[source] = [:]
                adjustedProbabilities[source][tag] = NerdUtils.getProportionOf(score, tagGroupMap.find {
                    it.value.applies(tag)
                }.value.tags().findAll({
                    it.getTagName() != tag
                }).collect({
                    tagScores[source].get(it.getTagName())
                }))
            }
        }
        return adjustedProbabilities
    }

    /**
     * Get tag correlations by text
     *
     * @param eventDate
     * @return
     */
    def getTextKeywordsByDate(Date eventDate) {
        Logger.log(String.format('Attempting to get association for %s', eventDate))
        ExtractedText extractedText = textExtractorService.extractTextForDate(eventDate)
        TextAssociations textAssociations = new TextAssociations()
        extractedText.extract(ExtractedText.TextSource.TWITTER)?.unique()?.each { String word ->
            String source = ExtractedText.TextSource.TWITTER as String
            BrainCount brainCount = bytesFetcherService.getBrainCount(
                    CaptureTextAssociationsService.generateReference(
                            word,
                            source
                    ),
                    source
            )
            List<BrainCount> list = textAssociations.tagAssociations.get(ExtractedText.TextSource.TWITTER)
            if(!list) list = []
            list.add(brainCount)
            textAssociations.tagAssociations.put(ExtractedText.TextSource.TWITTER, list)
        }
        extractedText.extract(ExtractedText.TextSource.NEWS)?.unique()?.each { String word ->
            tagMap.each { def tag ->
                String source = ExtractedText.TextSource.NEWS as String
                BrainCount brainCount = bytesFetcherService.getBrainCount(
                        CaptureTextAssociationsService.generateReference(
                                word,
                                source
                        ),
                        source
                )
                List<BrainCount> list = textAssociations.tagAssociations.get(ExtractedText.TextSource.NEWS)
                if(!list) list = []
                list.add(brainCount)
                textAssociations.tagAssociations.put(ExtractedText.TextSource.NEWS, list)
            }
        }
        return textAssociations
    }

}
