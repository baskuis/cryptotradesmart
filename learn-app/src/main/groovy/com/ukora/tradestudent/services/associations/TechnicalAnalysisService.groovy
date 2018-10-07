package com.ukora.tradestudent.services.associations

import com.ukora.tradestudent.entities.Memory
import com.ukora.tradestudent.entities.PriceEntry
import com.ukora.tradestudent.services.BytesFetcherService
import com.ukora.tradestudent.utils.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * The logic could also belong to the CaptureAssociationsService
 * but since it's a bit more involved - it's broken out into a separate service
 *
 */
@Service
class TechnicalAnalysisService {

    @Autowired
    BytesFetcherService bytesFetcherService

    public final String REFERENCE_NAME = 'technicalAnalysis'

    private static final int INTERVAL_SECONDS = 60
    private static final int MAX_HISTORICAL_REFERENCE_HOURS = 36

    private static final int MIN_DISTANCE_FROM_PEAK = 20

    private static TreeMap<Long, Double> priceCache = [:]

    private static final List<Integer> analysisBoundaries = [
            30,
            60,
            90,
            120,
            480,
            720
    ]

    /**
     * Extract technical analysis from moment in time
     * We'll calculate the relative distance to from the lower to upper boundary
     * for each of the analysis intervals
     *
     * @param atDate
     * @return
     */
    Map<String, Double> extractTechnicalAnalysis(Date atDate) {

        /** The key is to generate technical analysis references */
        Map<String, Double> technicalAnalysisReferences = [:]

        /** Define range from which to get prices */
        Instant from = Instant.ofEpochMilli(atDate.time) - (Duration.ofHours(MAX_HISTORICAL_REFERENCE_HOURS))

        /** Retrieve price entries */
        List<PriceEntry> priceEntries = getReferences(Date.from(from), atDate)

        /** Quick assertion */
        if (!priceEntries || priceEntries.size() < 100) {
            Logger.log('No value priceEntries')
            return null
        }

        /** Create references */
        analysisBoundaries.each {
            String reference = CaptureAssociationsService.INSTANT + CaptureAssociationsService.SEP + REFERENCE_NAME + CaptureAssociationsService.SEP + it
            Map<String, Double> analysisValues = extractAnalysis(it, priceEntries)
            if (analysisValues) {
                analysisValues.each {
                    technicalAnalysisReferences.put(reference + CaptureAssociationsService.SEP + it.key, it.value)
                }
            }
        }

        return technicalAnalysisReferences

    }

    /**
     * Drop price cache
     */
    @Scheduled(fixedRate = 300000l)
    static void dropPriceCache(){
        priceCache = new ConcurrentHashMap<>()
    }

    /**
     * Extract analysis value from price entries
     *
     * @param boundary
     * @param priceEntries
     * @return
     */
    private static Map<String,Double> extractAnalysis(Integer boundary, List<PriceEntry> priceEntries) {

        PriceEntry finalEntry = priceEntries.last()

        List<PriceEntry> reversed = priceEntries.reverse()

        int counter = 0
        PriceEntry earlyHighest
        PriceEntry lateHighest
        PriceEntry earlyLowest
        PriceEntry lateLowest

        try {
            reversed.each {
                if (MIN_DISTANCE_FROM_PEAK < counter) {
                    if (boundary + MIN_DISTANCE_FROM_PEAK > counter) {
                        if (!lateHighest || lateHighest.price < it.price) {
                            lateHighest = it
                        }
                        if (!lateLowest || lateLowest.price > it.price) {
                            lateLowest = it
                        }
                    }
                }
                if (boundary + (2 * MIN_DISTANCE_FROM_PEAK) < counter) {
                    if (2 * boundary + (2 * MIN_DISTANCE_FROM_PEAK) > counter) {
                        if (!earlyHighest || earlyHighest.price < it.price) {
                            earlyHighest = it
                        }
                        if (!earlyLowest || earlyLowest.price > it.price) {
                            earlyLowest = it
                        }
                    } else {
                        throw new RuntimeException('Past boundary')
                    }
                }
                counter++
            }
        } catch (RuntimeException e) {
            /** Ignore */
        }

        /** Assert we found thresholds */
        if (!earlyHighest || !earlyLowest || !lateHighest || !lateLowest) {
            Logger.log('No earlyHighest earlyLowest lateHighest lateLowest')
            return null
        }

        /** Digest values - project values at current price latestEntry */
        Double topTrendProjection = (
                (lateHighest.price - earlyHighest.price) /
                        (lateHighest.date.time - earlyHighest.date.time) *
                        (finalEntry.date.time - lateHighest.date.time)
        ) + lateHighest.price
        Double bottomTrendProjection = (
                (lateLowest.price - earlyLowest.price) /
                        (lateLowest.date.time - earlyLowest.date.time) *
                        (finalEntry.date.time - lateLowest.date.time)
        ) + lateHighest.price
        Double deltaTrendProjection = topTrendProjection - bottomTrendProjection

        /** Assert valid numerical values */
        if(!topTrendProjection || topTrendProjection.naN || !bottomTrendProjection || bottomTrendProjection.naN || !deltaTrendProjection || deltaTrendProjection.naN){
            Logger.log('Invalid topTrendProjection | bottomTrendProjection | deltaTrendProjection')
            return null
        }

        Double normalizedTopTrendProjection = topTrendProjection / finalEntry.price
        Double normalizedBottomTrendProjection = bottomTrendProjection / finalEntry.price
        Double normalizedDeltaTrendProjection = deltaTrendProjection / finalEntry.price
        Double normalizedTopDistance = (topTrendProjection - finalEntry.price) / finalEntry.price
        Double normalizedBottomDistance = (finalEntry.price - bottomTrendProjection) / finalEntry.price

        if(
            normalizedTopTrendProjection < 0 || normalizedTopTrendProjection > 2 ||
            normalizedBottomTrendProjection < 0 || normalizedBottomTrendProjection > 2 ||
            normalizedDeltaTrendProjection < -1 || normalizedDeltaTrendProjection > 1 ||
            normalizedTopDistance < -1 || normalizedTopDistance > 1 ||
            normalizedBottomDistance < -1 || normalizedBottomDistance > 1
        ){
            Logger.log(String.format('Invalid values for technical analysis ' +
                    'normalizedTopTrendProjection:%s normalizedBottomTrendProjection:%s ' +
                    'normalizedDeltaTrendProjection:%s normalizedTopDistance:%s ' +
                    'normalizedBottomDistance:%s',
                    normalizedTopTrendProjection,
                    normalizedBottomTrendProjection,
                    normalizedDeltaTrendProjection,
                    normalizedTopDistance,
                    normalizedBottomDistance))
            return null
        }

        return [
                'normalizedTopTrendProjection': normalizedTopTrendProjection,
                'normalizedBottomTrendProjection': normalizedBottomTrendProjection,
                'normalizedDeltaTrendProjection': normalizedDeltaTrendProjection,
                'normalizedTopDistance': normalizedTopDistance,
                'normalizedBottomDistance': normalizedBottomDistance
        ]

    }

    /**
     * Extract memory from db
     * for requested time range
     *
     * @param fromDate
     * @return
     */
    private List<PriceEntry> getReferences(Date fromDate, Date toDate) {
        if (!fromDate) return null
        Instant end = Instant.ofEpochMilli((Math.floor(toDate.time / 10000) * 10000) as Long)
        Duration gap = Duration.ofSeconds(INTERVAL_SECONDS)
        Instant current = Instant.ofEpochMilli((Math.floor(fromDate.time / 10000) * 10000) as Long)
        List<PriceEntry> priceEntries = []
        while (current.isBefore(end)) {
            current = current + gap
            Double cachedPrice = priceCache.get(current.epochSecond)
            if(cachedPrice){
                priceEntries << new PriceEntry(
                        date: Date.from(current),
                        price: cachedPrice
                )
                continue
            }
            Memory memory = bytesFetcherService.getMemory(Date.from(current))
            if (memory && memory.graph.price) {
                if (!memory.metadata?.datetime || !memory.graph?.price) {
                    continue
                }
                priceCache.put(current.epochSecond, memory.graph.price)
                priceEntries << new PriceEntry(
                        date: memory.metadata.datetime,
                        price: memory.graph.price
                )
            }
        }
        return priceEntries
    }

}
