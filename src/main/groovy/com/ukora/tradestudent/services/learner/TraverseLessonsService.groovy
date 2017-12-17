package com.ukora.tradestudent.services.learner

import com.ukora.tradestudent.entities.Lesson
import com.ukora.tradestudent.entities.Memory
import com.ukora.tradestudent.entities.Property
import com.ukora.tradestudent.services.BytesFetcherService
import com.ukora.tradestudent.tags.buysell.BuySellTagGroup
import com.ukora.tradestudent.tags.trend.UpDownTagGroup
import com.ukora.tradestudent.utils.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class TraverseLessonsService {

    static boolean running = false

    /** Represents trade fee - and trade risk 0.3% over regular 0.2% trade fee */
    private final static Double TRADE_LOSS = 0.005

    /** Minimal gain for trade */
    private final static Double MINIMAL_GAIN = 1.0075

    private final static Double MINIMAL_PROPORTION = 1.15

    public final static long INTERVAL_SECONDS = 60
    public final static long INTERVAL_HOURS = 1

    public final static long REPEAT_FOR_TREND = 8 /** Represents hours */
    public final static long MINIMUM_HOLD_PERIOD = 7 /** In minutes */
    public final static long REPEAT_FOR_BUY_SELL = 90 /** Represents minutes */

    private final static int SIMULATION_INTERVAL_INCREMENT = 1

    private final static int PEAK_PADDING = 5

    public final static String LATEST_BUY_SELL_PROPERTY_KEY = 'latestBuySell'
    public final static String LATEST_UP_DOWN_PROPERTY_KEY = 'latestUpDown'

    @Autowired
    UpDownTagGroup upDownTagGroup

    @Autowired
    BuySellTagGroup buySellTagGroup

    @Autowired
    BytesFetcherService bytesFetcherService

    List<LearnSimulation> learnSimulations = []

    @PostConstruct
    void init() {
        for (int i = MINIMUM_HOLD_PERIOD; i <= REPEAT_FOR_BUY_SELL; i += SIMULATION_INTERVAL_INCREMENT) {
            learnSimulations << new LearnSimulation(
                    interval: i
            )
        }
    }

    /**
     * Learn from trading history
     *
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Async
    void learnFromHistoryTrendData() {
        if (!running) {
            running = true
            Property latestUpDown = bytesFetcherService.getProperty(LATEST_UP_DOWN_PROPERTY_KEY)
            Instant start = Instant.now().minus(14, ChronoUnit.DAYS)
            if (latestUpDown) {
                start = new Date(latestUpDown.getValue()).toInstant().minus(16, ChronoUnit.HOURS)
            }
            learnFromTrend(Date.from(start))
            bytesFetcherService.saveProperty(LATEST_UP_DOWN_PROPERTY_KEY, new Date() as String)
            Logger.log(String.format("Completed"))
            running = false
        } else {
            Logger.log("already learning from memory")
        }
    }

    /**
     * Learn from trading history
     *
     */
    @Scheduled(cron = "0 0 */4 * * *")
    @Async
    void learnFromBuySellBehavior() {
        if (!running) {
            running = true
            Property latestBuySell = bytesFetcherService.getProperty(LATEST_BUY_SELL_PROPERTY_KEY)
            Instant start = Instant.now().minus(14, ChronoUnit.DAYS)
            if (latestBuySell) {
                start = new Date(latestBuySell.getValue()).toInstant().minus(24, ChronoUnit.HOURS)
            }
            learnFromBuySell(Date.from(start))
            bytesFetcherService.saveProperty(LATEST_BUY_SELL_PROPERTY_KEY, new Date() as String)
            Logger.log(String.format("Completed"))
            running = false
        } else {
            Logger.log("already learning from memory")
        }
    }

    /**
     * Learn from buy sell
     *
     * @param fromDate
     */
    void learnFromBuySell(Date fromDate) {

        Logger.log(String.format('Learning from buy/sell starting from %s', fromDate))

        /** Step 1.) Extract data points */
        Logger.log(String.format("Extracting data points since %s", fromDate))
        Map<Date, Double> references = getReferences(fromDate)
        Logger.log(String.format("Extracted %s data points", references.size()))
        if (!references || references.size() == 0) return

        /** Step 2.) Transform references */
        List<Map<String, Object>> transformedReferences = []
        references.each {
            transformedReferences << [
                    'date' : it.key,
                    'price': it.value
            ]
        }

        /** Step 3.) Walk trough price history and find opportunities, run simulations */
        learnSimulations.each { LearnSimulation learnSimulation ->
            runTradeSimulation(transformedReferences, learnSimulation)
        }

        /** Step 4.) Extract lessons from winning simulation */
        LearnSimulation winningSimulation = learnSimulations?.sort({ -it.balanceA })?.first()
        if (winningSimulation) {
            Logger.log("winningInterval: ${winningSimulation.interval} winning.balanceA: ${winningSimulation.balanceA}")
            if (winningSimulation.balanceA > MINIMAL_PROPORTION) {
                runTradeSimulation(transformedReferences, winningSimulation).eachWithIndex { Map<String, Object> entry, int index ->
                    if (entry['sell']) {
                        Logger.log("Storing sell lesson date: ${entry['date']} price: ${entry['price']}")
                        bytesFetcherService.saveLesson(new Lesson(
                                tag: buySellTagGroup.sellTag,
                                date: entry['date'] as Date,
                                price: entry['price'] as Double
                        ))
                    } else if (entry['buy']) {
                        Logger.log("Storing buy lesson date: ${entry['date']} price: ${entry['price']}")
                        bytesFetcherService.saveLesson(new Lesson(
                                tag: buySellTagGroup.buyTag,
                                date: entry['date'] as Date,
                                price: entry['price'] as Double
                        ))
                    }
                }
            } else {
                Logger.log("Unable to find willing simulation producing value ${winningSimulation.balanceA}")
            }
        }

    }

    static List<Map<String, Object>> runTradeSimulation(List<Map<String, Object>> transformedReferences, LearnSimulation learnSimulation) {
        Logger.log("Running simulation with interval ${learnSimulation.interval}")
        boolean previousRising = null
        boolean previousFalling = null
        List<Map<String, Object>> progression = []
        transformedReferences.eachWithIndex { Map<String, Object> reference, int index ->
            List<Map<String, Object>> previousEntries = []
            List<Map<String, Object>> nextEntries = []
            for (int i = PEAK_PADDING; i < learnSimulation.interval; i++) {
                try {
                    previousEntries << transformedReferences.get(index - i)
                } catch (IndexOutOfBoundsException e) { /** Ignore */
                }
                try {
                    nextEntries << transformedReferences.get(index + i)
                } catch (IndexOutOfBoundsException e) { /** Ignore */
                }
            }
            if (previousEntries.size() < (learnSimulation.interval / 2) || nextEntries.size() < (learnSimulation.interval / 2)) {
                Logger.debug("previousEntries:${previousEntries.size()} nextEntries:${nextEntries.size()} out of bounds")
                return
            }
            Map<String, Object> entry = [:]
            boolean rising = (nextEntries.size() > (learnSimulation.interval / 2) && nextEntries.findAll {
                it.get('price') > reference.get('price') * MINIMAL_GAIN
            }.size() > 0)
            boolean falling = (nextEntries.size() > (learnSimulation.interval / 2) && nextEntries.findAll {
                it.get('price') * MINIMAL_GAIN < reference.get('price')
            }.size() > 0)

            boolean risen = (previousEntries.size() > learnSimulation.interval / 2) && previousEntries.findAll {
                it.get('price') * MINIMAL_GAIN < reference.get('price')
            }.size() > 0
            boolean fallen = (previousEntries.size() > learnSimulation.interval / 2) && previousEntries.findAll {
                it.get('price') > reference.get('price') * MINIMAL_GAIN
            }.size() > 0

            boolean buy = fallen && rising
            boolean sell = risen && falling

            entry['date'] = reference.get('date')
            entry['price'] = reference.get('price')
            entry['rising'] = rising
            entry['falling'] = falling
            entry['risen'] = risen
            entry['fallen'] = fallen
            entry['buy'] = buy
            entry['sell'] = sell
            if (buy || sell) {
                Logger.debug(String.format("risen: %s, rising: %s, fallen: %s, falling: %s, buy: %s, sell: %s, date: %s, price: %s",
                        risen, rising, fallen, falling, buy, sell, entry['date'], entry['price']
                ))
            }
            progression << entry
            previousRising = rising
            previousFalling = falling
        }
        Double finalPrice = 0
        progression.each { Map<String, Object> entry ->
            if (entry['sell']) {
                learnSimulation.balanceB += ((learnSimulation.balanceA / PEAK_PADDING) * (entry['price'] as Double) * (1 - TRADE_LOSS))
                learnSimulation.balanceA = learnSimulation.balanceA * (1 - (1/PEAK_PADDING))
                learnSimulation.tradeCount++
            } else if (entry['buy']) {
                learnSimulation.balanceA += (((learnSimulation.balanceB / PEAK_PADDING) / (entry['price'] as Double)) * (1 - TRADE_LOSS))
                learnSimulation.balanceB = learnSimulation.balanceB * (1 - (1/PEAK_PADDING))
                learnSimulation.tradeCount++
            }
            finalPrice = (entry['price'] as Double)
        }
        learnSimulation.balanceA = learnSimulation.balanceA + ((learnSimulation.balanceB / finalPrice) * (1 - TRADE_LOSS))
        println "learnSimulation.balanceA: ${learnSimulation.balanceA} learnSimulation.tradeCount: ${learnSimulation.tradeCount}"
        return progression
    }

    /**
     * Learn from trends
     *
     * @param fromDate
     */
    void learnFromTrend(Date fromDate) {

        Logger.log(String.format('Learning from trends starting from %s', fromDate))

        /** Step 1.) Extract data points */
        Logger.log(String.format("Extracting data points since %s", fromDate))
        Map<Date, Double> references = getReferences(fromDate)
        Logger.log(String.format("Extracted %s data points", references.size()))
        if (!references || references.size() == 0) return

        /** Step 2.) Digest data points into reference object */
        List<Map<String, Object>> averages = digestReferences(fromDate, references)
        Logger.log(String.format("Extracted %s incremental summaries", averages.size()))

        /** Step 3.) Hydrate with trend information */
        Logger.log(String.format("Enriching averages map with trend indicators"))
        hydrateTrendIndicators(averages)

        /** Step 4.) Extract trend lessons */
        Logger.log(String.format("Extracting lessons"))
        List<Lesson> lessons = extractLessons(averages)
        Logger.log(String.format("Extracted %s lessons", lessons.size()))

        /** Step 5.) Learn from trend data - save the lessons */
        Logger.log(String.format("Saving lessons"))
        lessons.each {
            bytesFetcherService.saveLesson(it)
        }

    }

    /**
     * Extract lessons
     *
     * @param averages
     * @return
     */
    private List<Lesson> extractLessons(List<Map<String, Object>> averages) {
        List<Lesson> lessons = []
        averages.each {
            def average = it
            (average['entries'] as Map).each {
                boolean up = false
                if (average['upReversal']) {
                    up = (it.key as Date).after(average['lowest']['date'] as Date)
                } else if (average['downReversal']) {
                    up = (it.key as Date).before(average['highest']['date'] as Date)
                } else if (average['rising']) {
                    up = true
                } else if (average['falling']) {
                    up = false
                }
                lessons << new Lesson(
                        tag: up ? upDownTagGroup.upTag : upDownTagGroup.downTag,
                        date: it.key as Date,
                        price: it.value as Double
                )
            }
        }
        return lessons.unique({ a, b -> a.date <=> b.date })
    }

    /**
     * Enrich averages data with trend information
     *
     * @param averages
     * @return
     */
    private static void hydrateTrendIndicators(List<Map<String, Object>> averages) {
        boolean previousRising = null
        boolean previousFalling = null
        averages.eachWithIndex { Map average, int index ->
            List previousEntries = []
            List nextEntries = []
            for (int i = REPEAT_FOR_TREND; i > 0; i--) {
                try {
                    previousEntries << averages.get(index - i)
                } catch (IndexOutOfBoundsException e) { /** Ignore */
                }
                try {
                    nextEntries << averages.get(index + i)
                } catch (IndexOutOfBoundsException e) { /** Ignore */
                }
            }
            boolean rising = (nextEntries.size() > (REPEAT_FOR_TREND / 2) && nextEntries.collect { it['average'] }.sum() / nextEntries.size() > average['average'])
            boolean falling = !rising
            average['rising'] = rising
            average['falling'] = falling
            boolean upReversal = (previousFalling != null && previousFalling != falling && rising && previousRising != rising)
            boolean downReversal = (previousRising != null && previousRising != rising && falling && previousFalling != falling)
            average['upReversal'] = upReversal
            average['downReversal'] = downReversal
            Logger.log(String.format("rising: %s, falling: %s, upReversal: %s, downReversal: %s, date: %s, avgPrice: %s",
                    rising, falling, upReversal, downReversal, average['date'], average['average']
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
        return reference.sort({ it.key })
    }

}
