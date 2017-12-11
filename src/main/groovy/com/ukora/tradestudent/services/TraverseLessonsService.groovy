package com.ukora.tradestudent.services

import com.ukora.tradestudent.entities.Memory
import com.ukora.tradestudent.tags.trend.UpDownTagGroup
import com.ukora.tradestudent.utils.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class TraverseLessonsService {

    static boolean running = false

    public final static long INTERVAL_SECONDS = 60

    public final static long INTERVAL_HOURS = 2
    public final static long INTERVAL_FOR_TREND = 10

    /*@Autowired*/
    UpDownTagGroup upDownTagGroup

    @Autowired
    BytesFetcherService bytesFetcherService

    @Autowired
    ProbabilityFigurerService probabilityFigurerService

    //@Scheduled(cron = "*/5 * * * * *")
    void learn(){
        if(!running){
            running = true
            Logger.log('traverse memory')
            Instant start = Instant.now().minus(7, ChronoUnit.DAYS)
            learnFromHistory(Date.from(start))
        }
        running = false
    }

    void learnFromHistory(Date fromDate){
        if (!fromDate) return
        Instant end = Instant.now()
        Duration gap = Duration.ofSeconds(INTERVAL_SECONDS)
        Duration intervalGap = Duration.ofHours(INTERVAL_HOURS)

        Instant current = Instant.ofEpochMilli(fromDate.time)
        while (current.isBefore(end)) {
            current = current + gap
            Memory memory = bytesFetcherService.getMemory(Date.from(current))

        }
    }

}
