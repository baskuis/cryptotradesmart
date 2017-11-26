package com.ukora.tradestudent.services

import com.ukora.tradestudent.entities.CorrelationAssociation
import com.ukora.tradestudent.tags.AbstractCorrelationTag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

@Service
class ProbabilityFigurerService {

    @Autowired
    CaptureAssociationsService captureAssociationsService

    @Autowired
    BytesFetcherService bytesFetcherService

    @Autowired
    ApplicationContext applicationContext

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
        bytesFetcherService.hydrateAssociation(correlationAssociation)
        applicationContext.getBeansOfType(AbstractCorrelationTag).each {
            captureAssociationsService.hydrateAssociationTags(correlationAssociation, it.value.getTagName())
        }
        return correlationAssociation
    }

}
