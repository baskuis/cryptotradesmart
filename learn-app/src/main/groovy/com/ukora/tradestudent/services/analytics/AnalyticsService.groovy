package com.ukora.tradestudent.services.analytics

import com.ukora.domain.entities.CorrelationAssociation
import com.ukora.tradestudent.services.ProbabilityCombinerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import java.time.LocalDateTime
import java.time.ZoneId

@Service
class AnalyticsService {

    @Autowired
    ProbabilityCombinerService probabilityCombinerService

    void buildAnalyticsHistory() {
        CorrelationAssociation correlationAssociation = probabilityCombinerService.getCorrelationAssociations(
                Date.from(LocalDateTime.now().minusDays(2).atZone(ZoneId.systemDefault()).toInstant())
        )
        correlationAssociation.tagProbabilities.each {

        }
    }

}