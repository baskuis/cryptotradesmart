package com.ukora.tradestudent.services

import com.ukora.tradestudent.entities.Memory
import com.ukora.tradestudent.tags.trend.UpDownTagGroup
import com.ukora.tradestudent.utils.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
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
    public final static long REPEAT_FOR_TREND = 8
    public final static long MINIMAL_DIFFERENTIAL = 0.005

    /*@Autowired*/
    UpDownTagGroup upDownTagGroup

    @Autowired
    BytesFetcherService bytesFetcherService

    @Autowired
    ProbabilityFigurerService probabilityFigurerService

    //@Scheduled(cron = "*/5 * * * * *")
    @Async
    void learn(){
        if(!running){
            running = true
            Instant start = Instant.now().minus(7, ChronoUnit.DAYS)
            Logger.log(String.format('traverse memory staring from %s', Date.from(start)))
            learnFromHistory(Date.from(start))
            running = false
        }else{
            Logger.log("already learning from memory")
        }
    }

    void learnFromHistory(Date fromDate){

        /**
         * Get prices
         */
        if (!fromDate) return
        Instant end = Instant.now()
        Duration gap = Duration.ofSeconds(INTERVAL_SECONDS)
        Instant current = Instant.ofEpochMilli(fromDate.time)
        Map<Date, Double> reference = [:]
        while (current.isBefore(end)) {
            current = current + gap
            Memory memory = bytesFetcherService.getMemory(Date.from(current))
            if(memory && memory.graph.price) {
                reference.put(Date.from(current), memory.graph.price)
            }
        }

        /**
         * Get averages
         */
        List<Map<String, Object>> averages = []
        Duration hourGap = Duration.ofHours(INTERVAL_HOURS)
        current = Instant.ofEpochMilli(fromDate.time)
        while (current.isBefore(end)) {
            Double average
            Double total = 0d
            Map<Date, Double> entries = reference.findAll {
                it.key.after(Date.from(current)) && it.key.before(Date.from(current + hourGap))
            }.each {
                total += it.value
            }
            average = total / entries.size()
            if(!average.naN) {
                averages << [
                        'date'   : Date.from(current),
                        'average': average,
                        'entries': entries
                ]
            }
            current = current + hourGap
        }

        println averages

    }

}
