package com.ukora.tradestudent.services

import com.ukora.tradestudent.utils.Logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class TraverseLessonsService {

    @Scheduled(cron = "*/5 * * * * *")
    void learn(){

        Logger.debug('traverse memory')

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
