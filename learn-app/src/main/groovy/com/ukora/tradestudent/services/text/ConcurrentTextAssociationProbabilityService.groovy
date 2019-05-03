package com.ukora.tradestudent.services.text

import com.ukora.domain.entities.TextCorrelationAssociation
import com.ukora.tradestudent.services.BytesFetcherService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ConcurrentTextAssociationProbabilityService {

    @Autowired
    TextAssociationProbabilityService textAssociationProbabilityService

    @Autowired
    BytesFetcherService bytesFetcherService

    @Scheduled(initialDelay = 300000l, fixedRate = 300000l)
    @CacheEvict(value = "textCorrelationAssociation", allEntries = true)
    void flush() { }

    synchronized tagCorrelationByText(Date eventDate){
        return textAssociationProbabilityService.tagCorrelationByText(eventDate)
    }

    @Cacheable(value = 'textCorrelationAssociation')
    TextCorrelationAssociation getCorrelationAssociations(Date eventDate) {
        Calendar cal = Calendar.getInstance()
        cal.setTime(eventDate)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        def strategyProbabilities = textAssociationProbabilityService.tagCorrelationByText(Date.from(cal.toInstant()))
        TextCorrelationAssociation textCorrelationAssociation = null
        if (strategyProbabilities) {
            textCorrelationAssociation = new TextCorrelationAssociation(
                    strategyProbabilities: strategyProbabilities
            )
            bytesFetcherService.captureTextCorrelationAssociation(textCorrelationAssociation)
        }
        return textCorrelationAssociation
    }

}
