package com.ukora.tradestudent.services.text

import com.ukora.tradestudent.entities.BrainCount
import com.ukora.tradestudent.entities.ExtractedText
import com.ukora.tradestudent.entities.KeywordAssociation
import com.ukora.tradestudent.services.BytesFetcherService
import com.ukora.tradestudent.services.TagService
import com.ukora.tradestudent.services.associations.text.CaptureTextAssociationsService
import com.ukora.tradestudent.tags.AbstractCorrelationTag
import com.ukora.tradestudent.tags.TagGroup
import com.ukora.tradestudent.utils.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct

@Service
class KeywordAssociationService {

    @Autowired
    TagService tagService

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    BytesFetcherService bytesFetcherService

    Map<String, AbstractCorrelationTag> tagMap = [:]
    Map<String, Integer> tagCount = [:]

    @PostConstruct
    void init() {

        /** Collect primary tags */
        applicationContext.getBeansOfType(AbstractCorrelationTag).each {
            Logger.log(String.format("Found tag %s", it.key))
            tagMap.put(it.key, it.value)
        }

        refresh()

    }

    @Scheduled(initialDelay = 300000l, fixedRate = 300000l)
    @CacheEvict(value = "keywordAssociation", allEntries = true)
    void refresh() {
        tagMap.each {
            Integer c = bytesFetcherService.getLessonCount(it.value.tagName)
            if(c) tagCount.put(it.value.tagName, c)
        }
    }

    /**
     * Get keyword association
     *
     * @param keyword
     * @return
     */
    @Cacheable("keywordAssociation")
    KeywordAssociation getKeywordAssociation(String keyword, ExtractedText.TextSource source){
        BrainCount brainCount = bytesFetcherService.getBrainCount(
                CaptureTextAssociationsService.generateReference(keyword, source as String),
                source as String
        )
        if(brainCount) {
            KeywordAssociation keywordAssociation = new KeywordAssociation()
            keywordAssociation.source = source
            tagMap.each {
                AbstractCorrelationTag tag = it.value
                TagGroup tagGroup = tagService.getTagGroupByTagName(it.value.tagName)
                if(tag && tagGroup.tags().size() == 2){
                    AbstractCorrelationTag counterTag = tagGroup.tags().find({
                        it.tagName != tag.tagName
                    })
                    Double p = 1 / tagGroup.tags().size()
                    Integer tagKeywordAssociationCount = brainCount.counters.get(tag.tagName)
                    Integer counterTagKeywordAssociationCount = brainCount.counters.get(counterTag.tagName)
                    if(counterTag && tagKeywordAssociationCount && counterTagKeywordAssociationCount){
                        Double tagProportion = tagCount.getOrDefault(counterTag.tagName, 1) /
                                (tagCount.getOrDefault(tag.tagName, 1) + tagCount.getOrDefault(counterTag.tagName, 1))
                        Double counterTagProportion = 1 - tagProportion
                        p = (tagProportion * tagKeywordAssociationCount) / (
                                (tagProportion * tagKeywordAssociationCount) +
                                        (counterTagProportion * counterTagKeywordAssociationCount)
                        )
                    }
                    keywordAssociation.tagProbabilities.put(tag.tagName, p)
                }
            }
            return keywordAssociation
        }
        return null
    }

}
