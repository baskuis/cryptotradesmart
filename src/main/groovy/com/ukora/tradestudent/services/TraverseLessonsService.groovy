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

    public final static long INTERVAL_HOURS = 1
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
    void learn() {
        if (!running) {
            running = true
            Instant start = Instant.now().minus(70, ChronoUnit.DAYS)
            Logger.log(String.format('traverse memory staring from %s', Date.from(start)))
            learnFromHistory(Date.from(start))
            running = false
        } else {
            Logger.log("already learning from memory")
        }
    }

    /**
     * Learn from trading history
     *
     * @param fromDate
     */
    void learnFromHistory(Date fromDate) {

        Logger.log(String.format("Extracting data points since %s", fromDate))
        Map<Date, Double> references = getReferences(fromDate)
        Logger.log(String.format("Extracted %s data points", references.size()))

        List<Map<String, Object>> averages = digestReferences(fromDate, references)
        Logger.log(String.format("Extracted %s incremental summaries", averages.size()))

        /** Extract trend information */
        boolean previousRising = null
        boolean previousFalling = null
        averages.eachWithIndex { Map average, int index ->

            List previousEntries = []
            List nextEntries = []
            for (int i = REPEAT_FOR_TREND; i > 0; i--) {
                try { previousEntries << averages.get(index - i) } catch (IndexOutOfBoundsException e) { /** Ignore */ }
                try { nextEntries << averages.get(index + i) } catch (IndexOutOfBoundsException e) { /** Ignore */ }
            }
            boolean rising = (nextEntries.size() > (REPEAT_FOR_TREND / 2) && nextEntries.findAll { it['average'] > average['average'] }.size() > 0)
            boolean falling = (nextEntries.size() > (REPEAT_FOR_TREND / 2) && nextEntries.findAll { it['average'] < average['average'] }.size() == nextEntries.size())

            boolean upReversal = (previousFalling != null && previousFalling != falling)
            boolean downReversal = (previousRising != null && previousRising != rising)

            Logger.log(String.format("rising: %s, falling: %s, upReversal: %s, downReversal: %s, date: %s",
                rising, falling, upReversal, downReversal, average['date']
            ))

            previousRising = rising
            previousFalling = falling

        }

    }

    /**
     * Extract averages from data points
     *
     * @param fromDate
     * @param references
     * @return
     */
    private static List<Map<String, Object>> digestReferences(Date fromDate, Map<Date, Double> references) {
        Instant end = Instant.now()
        List<Map<String, Object>> averages = []
        Duration hourGap = Duration.ofHours(INTERVAL_HOURS)
        Instant current = Instant.ofEpochMilli(fromDate.time)
        while (current.isBefore(end)) {
            Double average
            Double total = 0d
            Map highest = [
                    'date' : null,
                    'value': 0d
            ]
            Map lowest = [
                    'date' : null,
                    'value': null
            ]
            Map<Date, Double> entries = references.findAll {
                it.key.after(Date.from(current)) && it.key.before(Date.from(current + hourGap))
            }.each {
                total += it.value
                if (!lowest['value'] || it.value < (lowest['value'] as Double)) {
                    lowest['date'] = it.key
                    lowest['value'] = it.value
                }
                if (it.value > (highest['value'] as Double)) {
                    highest['date'] = it.key
                    highest['value'] = it.value
                }
            }
            average = total / entries.size()
            if (!average.naN) {
                averages << [
                        'date'   : Date.from(current),
                        'average': average,
                        'entries': entries as TreeMap,
                        'lowest' : lowest,
                        'highest': highest
                ]
            }
            current = current + hourGap
        }
        return averages
    }

    /**
     * Extract memory from db
     * for requested time range
     *
     * @param fromDate
     * @return
     */
    private Map<Date, Double> getReferences(Date fromDate) {
        if (!fromDate) return null
        Instant end = Instant.now()
        Duration gap = Duration.ofSeconds(INTERVAL_SECONDS)
        Instant current = Instant.ofEpochMilli(fromDate.time)
        Map<Date, Double> reference = [:]
        while (current.isBefore(end)) {
            current = current + gap
            Memory memory = bytesFetcherService.getMemory(Date.from(current))
            if (memory && memory.graph.price) {
                reference.put(Date.from(current), memory.graph.price)
            }
        }
        return reference
    }

}
