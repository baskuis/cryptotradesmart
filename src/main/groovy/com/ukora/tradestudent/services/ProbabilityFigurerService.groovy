package com.ukora.tradestudent.services

import com.ukora.tradestudent.entities.CorrelationAssociation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProbabilityFigurerService {

    @Autowired
    CaptureAssociationsService captureAssociationsService

    CorrelationAssociation determineProbability(Date eventDate){

        return CorrelationAssociation()

    }

}
