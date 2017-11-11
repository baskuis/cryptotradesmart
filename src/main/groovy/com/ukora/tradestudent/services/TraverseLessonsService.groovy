package com.ukora.tradestudent.services

import groovy.util.logging.Log4j2
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Log4j2
@Service
class TraverseLessonsService {

    @Scheduled(cron = "*/5 * * * * *")
    void learn(){
        println 'traverse memory'

        /**
         *
         * Get min max for time delta (4 minutes)
         *
         * go to next delta - get min and max
         *
         *
         *
         *
         *
         */

    }

}
