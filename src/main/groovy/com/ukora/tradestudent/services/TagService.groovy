package com.ukora.tradestudent.services

import com.ukora.tradestudent.tags.AbstractCorrelationTag
import com.ukora.tradestudent.tags.TagGroup
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

}
