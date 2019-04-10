package com.ukora.tradestudent.services.text

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ConcurrentTextAssociationProbabilityService {

    @Autowired
    TextAssociationProbabilityService textAssociationProbabilityService

    synchronized tagCorrelationByText(Date eventDate){
        return textAssociationProbabilityService.tagCorrelationByText(eventDate)
    }

}
