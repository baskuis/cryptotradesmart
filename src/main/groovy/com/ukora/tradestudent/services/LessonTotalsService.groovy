package com.ukora.tradestudent.services

import com.ukora.tradestudent.tags.AbstractCorrelationTag
import com.ukora.tradestudent.utils.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct

@Service
class LessonTotalsService {

    @Autowired
    BytesFetcherService bytesFetcherService

    @Autowired
    ApplicationContext applicationContext

    Map<String, AbstractCorrelationTag> tagMap = [:]

    @PostConstruct
    void init() {

        /** Collect primary tags */
        applicationContext.getBeansOfType(AbstractCorrelationTag).each {
            Logger.log(String.format("Found tag %s", it.key))
            tagMap.put(it.key, it.value)
        }

    }

    Map<AbstractCorrelationTag, Integer> getTagCountSummary(){
        Map<AbstractCorrelationTag, Integer> summary = [:]
        tagMap.each {
            summary.put(it.value, bytesFetcherService.getLessonCount(it.value.tagName))
        }
        return summary
    }

}
