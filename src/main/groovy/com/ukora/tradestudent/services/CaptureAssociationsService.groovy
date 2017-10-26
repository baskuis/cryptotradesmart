package com.ukora.tradestudent.services

import groovy.util.logging.Log4j2
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Log4j2
@Service
class CaptureAssociationsService {

    @Scheduled(cron = "* * * * * *")
    retrieveLesson(){
        println 'hi'
        log.info('Running scheduled retrieve lesson task')
    }

}
