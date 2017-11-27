package com.ukora.tradestudent.services

import com.ukora.tradestudent.entities.BrainNode
import com.ukora.tradestudent.entities.CorrelationAssociation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProbabilityFigurerService {

    @Autowired
    CaptureAssociationsService captureAssociationsService

    @Autowired
    BytesFetcherService bytesFetcherService

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
        captureAssociationsService.hydrateAssocations(correlationAssociation)
        return correlationAssociation
    }

    /**
     * Get brain nodes
     *
     * @return
     */
    Map<String, BrainNode> getBrainNodes(){
        return bytesFetcherService.getAllBrainNodes()
    }

}
