package com.ukora.tradestudent.services

import com.ukora.tradestudent.entities.CorrelationAssociation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProbabilityFigurerService {

    @Autowired
    CaptureAssociationsService captureAssociationsService

    @Autowired
    BytesFetcherService bytesFetcherService

    CorrelationAssociation determineProbability(Date eventDate){
        CorrelationAssociation correlationAssociation = new CorrelationAssociation()
        correlationAssociation.setDate(eventDate)
        bytesFetcherService.hydrateAssociation(correlationAssociation)
        captureAssociationsService.hydrateAssociationTags(correlationAssociation, "buy") //TODO: get available tags from some sensible place
        captureAssociationsService.hydrateAssociationTags(correlationAssociation, "sell") //TODO: get available tags from some sensible place
        //TODO: Capture probablity of correlation on correlationAssocation
        return correlationAssociation
    }

}
