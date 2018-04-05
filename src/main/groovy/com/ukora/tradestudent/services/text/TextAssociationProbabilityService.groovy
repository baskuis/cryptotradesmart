package com.ukora.tradestudent.services.text

import com.ukora.tradestudent.entities.BrainCount
import com.ukora.tradestudent.entities.ExtractedText
import com.ukora.tradestudent.entities.TextAssociations
import com.ukora.tradestudent.services.BytesFetcherService
import com.ukora.tradestudent.services.associations.text.CaptureTextAssociationsService
import com.ukora.tradestudent.services.associations.text.TextExtractorService
import com.ukora.tradestudent.tags.AbstractCorrelationTag
import com.ukora.tradestudent.tags.TagGroup
import com.ukora.tradestudent.utils.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct

@Service
class TextAssociationProbabilityService {

    @Autowired
    BytesFetcherService bytesFetcherService

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    TextExtractorService textExtractorService

    Map<String, TagGroup> tagGroupMap = [:]
    Map<String, AbstractCorrelationTag> tagMap = [:]

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

    }

    /**
     * Get tag correlations by text
     *
     * @param eventDate
     * @return
     */
    def getTagCorrelationByText(Date eventDate) {

        Logger.log(String.format('Attempting to get association for %s', eventDate))
        ExtractedText extractedText = textExtractorService.extractTextForDate(eventDate)

        TextAssociations textAssociations = new TextAssociations()
        extractedText.extract(ExtractedText.TextSource.TWITTER).each { String word ->
            tagMap.each { def tag ->
                String source = ExtractedText.TextSource.TWITTER as String
                BrainCount brainCount = bytesFetcherService.getBrainCount(
                    CaptureTextAssociationsService.generateReference(
                        word,
                        tag.value.tagName,
                        source
                    ),
                    tag.value.tagName,
                    source
                )
                BrainCount generalBrainCount = bytesFetcherService.getBrainCount(
                    CaptureTextAssociationsService.generateReference(
                        word,
                        CaptureTextAssociationsService.GENERAL,
                        source
                    ),
                    tag.value.tagName,
                    source
                )
                if (brainCount && generalBrainCount) {
                    if (brainCount.count > 0) {
                        List<BrainCount> list = textAssociations.tagAssociations.get(tag.value)
                        if(!list) list = []
                        list.add(brainCount)
                        textAssociations.tagAssociations.put(tag.value, list)
                    }
                    if (generalBrainCount.count > 0) {
                        textAssociations.generalAssociations.add(generalBrainCount)
                    }
                }
            }
        }
        extractedText.extract(ExtractedText.TextSource.NEWS).each { String word ->
            tagMap.each { def tag ->
                String source = ExtractedText.TextSource.NEWS as String
                BrainCount brainCount = bytesFetcherService.getBrainCount(
                        CaptureTextAssociationsService.generateReference(
                                word,
                                tag.value.tagName,
                                source
                        ),
                        tag.value.tagName,
                        source
                )
                BrainCount generalBrainCount = bytesFetcherService.getBrainCount(
                        CaptureTextAssociationsService.generateReference(
                                word,
                                CaptureTextAssociationsService.GENERAL,
                                source
                        ),
                        tag.value.tagName,
                        source
                )
                if (brainCount && generalBrainCount) {
                    textAssociations.tagAssociations.getOrDefault(tag, []).add(brainCount)
                    textAssociations.generalAssociations.add(generalBrainCount)
                }
            }
        }

        return textAssociations

    }

}
