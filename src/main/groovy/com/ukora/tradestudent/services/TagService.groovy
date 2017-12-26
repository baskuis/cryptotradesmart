package com.ukora.tradestudent.services

import com.ukora.tradestudent.bayes.numbers.NumberAssociation
import com.ukora.tradestudent.entities.BrainNode
import com.ukora.tradestudent.tags.AbstractCorrelationTag
import com.ukora.tradestudent.tags.TagGroup
import com.ukora.tradestudent.utils.NerdUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct

@Service
class TagService {

    Map<String, ? extends AbstractCorrelationTag> tagMap = [:]
    Map<String, TagGroup> tagGroupMap = [:]

    @Autowired
    ApplicationContext applicationContext

    @PostConstruct
    void init() {
        tagMap = applicationContext.getBeansOfType(AbstractCorrelationTag)
        tagGroupMap = applicationContext.getBeansOfType(TagGroup)
    }

    /**
     * Get Tag by name
     *
     * @param tagName
     * @return
     */
    AbstractCorrelationTag getTagByName(String tagName){
        return tagMap.find { it.value.tagName == tagName }?.value
    }

    /**
     * Get tag group by tag
     *
     * @param tag
     * @return
     */
    TagGroup getTagGroupByTag(AbstractCorrelationTag tag){
        return tagGroupMap.find { (it.value.tags().find{ it.tagName == tag.tagName }) }?.value
    }

    /**
     * Get tag group by tag name
     *
     * @param tagName
     * @return
     */
    TagGroup getTagGroupByTagName(String tagName){
        return getTagGroupByTag(getTagByName(tagName))
    }

    /**
     * Get general number association from tag group
     *
     * @param reference
     * @param tagGroup
     * @return
     */
    static NumberAssociation getGeneralNumberAssociation(BrainNode brainNode, TagGroup tagGroup){
        if(!brainNode) return null
        if(!tagGroup) return null
        NumberAssociation generalNumberAssociation = new NumberAssociation()
        Double sumMean = 0
        List<Double> standardDeviations = []
        Double sumCount = 0
        Integer total = 0
        brainNode.tagReference.findAll({
            tagGroup.tags().collect({ it.tagName }).contains(it.key)
        }).each {
            sumMean += it.value.mean
            standardDeviations << it.value.standard_deviation
            sumCount += it.value.count
            total++
        }
        generalNumberAssociation.mean = sumMean / total
        generalNumberAssociation.standard_deviation = NerdUtils.combineStandardDeviations(standardDeviations as Double[])
        generalNumberAssociation.count = sumCount
        generalNumberAssociation.tagGroup = tagGroup.name
        generalNumberAssociation.tag = CaptureAssociationsService.GENERAL + CaptureAssociationsService.SEP + tagGroup.name
        return generalNumberAssociation
    }

}
