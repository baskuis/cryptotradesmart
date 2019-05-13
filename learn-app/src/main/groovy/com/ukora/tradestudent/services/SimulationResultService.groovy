package com.ukora.tradestudent.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.ukora.domain.entities.ExtractedText
import com.ukora.domain.entities.ExtractedText.TextSource
import com.ukora.domain.entities.SimulationResult
import com.ukora.tradestudent.strategy.probability.ProbabilityCombinerStrategy
import com.ukora.tradestudent.strategy.text.probablitity.TextProbabilityCombinerStrategy
import com.ukora.tradestudent.strategy.trading.flex.FlexTradeExecutionStrategy
import com.ukora.tradestudent.utils.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

@Service
class SimulationResultService {

    public final Integer MAX_AGE_IN_HOURS = 24

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    BytesFetcherService bytesFetcherService

    static final Integer DISTANCE_FROM_BINARY_SOFTENING_FACTOR = 20
    static final Integer THRESHOLD_BALANCE_SOFTENING_FACTOR = 10
    static final Double MINIMUM_DIFFERENTIAL = 1
    static final Double TRADE_COUNT_DIMINISHING_POWER = 0.30

    final int SECONDS_IN_HOUR = 3600

    List<String> textCombinerStrategies
    List<String> numericalCombinerStrategies
    List<String> textFlexTradeStrategies
    List<String> twitterFlexTradeStrategies
    List<String> newsFlexTradeStrategies
    List<String> numericalFlexTradeStrategies

    @PostConstruct
    def init() {
        textCombinerStrategies = applicationContext.getBeansOfType(TextProbabilityCombinerStrategy)?.keySet()?.toList()
        numericalCombinerStrategies = applicationContext.getBeansOfType(ProbabilityCombinerStrategy)?.keySet()?.toList()
        textFlexTradeStrategies = applicationContext.getBeansOfType(FlexTradeExecutionStrategy)?.findAll {
            it.value.getType() == FlexTradeExecutionStrategy.Type.TEXT
        }?.keySet()?.toList()
        numericalFlexTradeStrategies = applicationContext.getBeansOfType(FlexTradeExecutionStrategy)?.findAll {
            it.value.getType() == FlexTradeExecutionStrategy.Type.NUMERIC
        }?.keySet()?.toList()
        twitterFlexTradeStrategies = applicationContext.getBeansOfType(FlexTradeExecutionStrategy)?.findAll {
            it.value.getType() == FlexTradeExecutionStrategy.Type.TEXT &&
                    it.value.getTextSource() == ExtractedText.TextSource.TWITTER
        }?.keySet()?.toList()
        newsFlexTradeStrategies = applicationContext.getBeansOfType(FlexTradeExecutionStrategy)?.findAll {
            it.value.getType() == FlexTradeExecutionStrategy.Type.TEXT &&
                    it.value.getTextSource() == ExtractedText.TextSource.NEWS
        }?.keySet()?.toList()
    }

    /**
     * Get top performing simulation
     *
     * @return
     */
    SimulationResult getTopPerformingSimulation() {
        List<SimulationResult> r = getTopPerformingSimulations()
        r ? r.first() : null
    }

    /**
     * Get top performing probability combiners
     *
     * @return
     */
    List<String> getTopPerformingProbabilityCombiners() {
        List<String> performingProbabilityCombiners = []
        Map<String, Integer> ref = [:]
        bytesFetcherService.getSimulations()?.findAll({
            ref.put(it.probabilityCombinerStrategy, ref.getOrDefault(it.probabilityCombinerStrategy, 0) + 1)
        })
        ref.sort({ -it.value }).each { performingProbabilityCombiners.add(it.key) }
        performingProbabilityCombiners
    }

    /**
     * Get top performing trade execution strategies
     *
     * @return
     */
    List<String> getTopPerformingTradeExecutionStrategies() {
        List<String> performingTradeExecutionStrategies = []
        Map<String, Integer> ref = [:]
        bytesFetcherService.getSimulations()?.findAll({
            ref.put(it.tradeExecutionStrategy, ref.getOrDefault(it.tradeExecutionStrategy, 0) + 1)
        })
        ref.sort({ -it.value })
        ref.sort({ -it.value }).each { performingTradeExecutionStrategies.add(it.key) }
        performingTradeExecutionStrategies
    }

    static class SimulationRange {
        Map<String, Double> topTagGroupWeights = new ConcurrentHashMap<>()
        Map<String, Double> bottomTagGroupWeights = new ConcurrentHashMap<>()
        Double bottomBuyThreshold
        Double topBuyThreshold
        Double bottomSellThreshold
        Double topSellThreshold
    }

    SimulationRange getFlexTextSimulationRange() {
        SimulationRange simulationRange = new SimulationRange()
        bytesFetcherService.getSimulations()?.findAll({
            it.executionType == SimulationResult.ExecutionType.FLEX &&
                    it.differential > MINIMUM_DIFFERENTIAL &&
                    textFlexTradeStrategies?.contains(it.tradeExecutionStrategy)
        })?.sort({ SimulationResult a, SimulationResult b ->
            b.endDate <=> a.endDate
        })?.take(100)?.each({
            adjustRange(it, simulationRange)
        })
        return simulationRange
    }

    SimulationRange getFlexNumericalSimulationRange() {
        SimulationRange simulationRange = new SimulationRange()
        bytesFetcherService.getSimulations()?.findAll({
            it.executionType == SimulationResult.ExecutionType.FLEX &&
                    it.differential > MINIMUM_DIFFERENTIAL &&
                    this.numericalFlexTradeStrategies?.contains(it.tradeExecutionStrategy)
        })?.sort({ SimulationResult a, SimulationResult b ->
            b.endDate <=> a.endDate
        })?.take(100)?.each({
            adjustRange(it, simulationRange)
        })
        return simulationRange
    }

    static adjustRange(SimulationResult simulationResult, SimulationRange simulationRange) {
        if (simulationRange.topTagGroupWeights?.size() == 0) {
            simulationRange.topTagGroupWeights.putAll(simulationResult.tagGroupWeights)
        }
        if (simulationRange.bottomTagGroupWeights?.size() == 0) {
            simulationRange.bottomTagGroupWeights.putAll(simulationResult.tagGroupWeights)
        }
        if (!simulationRange.bottomBuyThreshold) {
            simulationRange.bottomBuyThreshold = simulationResult.sellThreshold
        }
        if (!simulationRange.bottomSellThreshold) {
            simulationRange.bottomSellThreshold = simulationResult.sellThreshold
        }
        if (!simulationRange.topBuyThreshold) {
            simulationRange.topBuyThreshold = simulationResult.buyThreshold
        }
        if (!simulationRange.topSellThreshold) {
            simulationRange.topSellThreshold = simulationResult.buyThreshold
        }
        simulationResult.tagGroupWeights.each {
            def tv = simulationRange.topTagGroupWeights.get(it.key)
            def bv = simulationRange.bottomTagGroupWeights.get(it.key)
            if (it.value > tv) {
                simulationRange.topTagGroupWeights.put(it.key, it.value)
            }
            if (it.value < bv) {
                simulationRange.bottomTagGroupWeights.put(it.key, it.value)
            }
        }
        simulationRange.bottomBuyThreshold = (simulationResult.buyThreshold < simulationRange.bottomBuyThreshold) ? simulationResult.buyThreshold : simulationRange.bottomBuyThreshold
        simulationRange.bottomSellThreshold = (simulationResult.sellThreshold < simulationRange.bottomSellThreshold) ? simulationResult.sellThreshold : simulationRange.bottomSellThreshold
        simulationRange.topBuyThreshold = (simulationResult.buyThreshold > simulationRange.topBuyThreshold) ? simulationResult.buyThreshold : simulationRange.topBuyThreshold
        simulationRange.topSellThreshold = (simulationResult.sellThreshold > simulationRange.topSellThreshold) ? simulationResult.sellThreshold : simulationRange.topSellThreshold
    }

    /**
     * Get top performing numerical flex simulation
     *
     * @return
     */
    SimulationResult getTopPerformingNumericalFlexSimulation() {
        def r = bytesFetcherService.getSimulations()?.findAll({
            it.executionType == SimulationResult.ExecutionType.FLEX &&
                    it.differential > MINIMUM_DIFFERENTIAL &&
                    this.numericalCombinerStrategies?.contains(it.probabilityCombinerStrategy) &&
                    this.numericalFlexTradeStrategies?.contains(it.tradeExecutionStrategy)
        })?.sort({ SimulationResult a, SimulationResult b ->
            b.endDate <=> a.endDate
        })?.take(100)?.sort({ SimulationResult a, SimulationResult b ->
            getScore(b) <=> getScore(a)
        })
        return r.size() > 0 ? r.first() : null
    }

    /**
     * Get top performing combined simulation
     *
     * @return
     */
    SimulationResult getTopPerformingCombinedSimulation(TextSource textSource) {
        def r = bytesFetcherService.getSimulations()?.findAll({
            it.executionType == SimulationResult.ExecutionType.COMBINED &&
                    it.differential > MINIMUM_DIFFERENTIAL &&
                    this.textFlexTradeStrategies?.contains(it.tradeExecutionStrategy) &&
                    (!textSource || (
                            (textSource == TextSource.NEWS && this.newsFlexTradeStrategies?.contains(it.tradeExecutionStrategy)) ||
                                    (textSource == TextSource.TWITTER && this.twitterFlexTradeStrategies?.contains(it.tradeExecutionStrategy))
                    ))
        })?.sort({ SimulationResult a, SimulationResult b ->
            b.endDate <=> a.endDate
        })?.take(100)?.sort({ SimulationResult a, SimulationResult b ->
            getScore(b) <=> getScore(a)
        })
        return r.size() > 0 ? r.first() : null
    }

    /**
     * Get top performing text flex simulation
     *
     * @return
     */
    SimulationResult getTopPerformingTextFlexSimulation(TextSource textSource) {
        def r = bytesFetcherService.getSimulations()?.findAll({
            it.executionType == SimulationResult.ExecutionType.FLEX &&
                    it.differential > MINIMUM_DIFFERENTIAL &&
                    this.textFlexTradeStrategies?.contains(it.tradeExecutionStrategy) &&
                    (!textSource || (
                            (textSource == TextSource.NEWS && this.newsFlexTradeStrategies?.contains(it.tradeExecutionStrategy)) ||
                                    (textSource == TextSource.TWITTER && this.twitterFlexTradeStrategies?.contains(it.tradeExecutionStrategy))
                    ))
        })?.sort({ SimulationResult a, SimulationResult b ->
            b.endDate <=> a.endDate
        })?.take(100)?.sort({ SimulationResult a, SimulationResult b ->
            getScore(b) <=> getScore(a)
        })
        return r.size() > 0 ? r.first() : null
    }

    /**
     * Get top performing simulations
     *
     * @return
     */
    List<SimulationResult> getTopPerformingSimulations() {
        return bytesFetcherService.getSimulations()?.findAll({
            it.endDate > Date.from(Instant.now().minusSeconds(MAX_AGE_IN_HOURS * SECONDS_IN_HOUR)) && it.differential > MINIMUM_DIFFERENTIAL
        })?.sort({ SimulationResult a, SimulationResult b ->
            b.endDate <=> a.endDate
        })?.take(100)?.sort({ SimulationResult a, SimulationResult b ->
            getScore(b) <=> getScore(a)
        })
    }

    /**
     * Sorting function
     *
     * @param simulationResult
     * @return
     */
    static getScore(SimulationResult simulationResult) {
        if (!simulationResult || !simulationResult?.tradeCount) {
            Logger.debug(String.format('Unable to get score for %s', new ObjectMapper().writeValueAsString(simulationResult)))
            return 0d
        }
        Double thresholdBalance = Math.abs(simulationResult.buyThreshold - simulationResult.sellThreshold)
        Double distanceFromBinary
        if (simulationResult.buyThreshold < simulationResult.sellThreshold) {
            distanceFromBinary = 1 - simulationResult.sellThreshold
        } else {
            distanceFromBinary = 1 - simulationResult.buyThreshold
        }
        Double modifiedDifferentialDelta = (simulationResult.differential - 1) * (1 - (distanceFromBinary / DISTANCE_FROM_BINARY_SOFTENING_FACTOR)) * (1 - (thresholdBalance / THRESHOLD_BALANCE_SOFTENING_FACTOR))
        return Math.pow(1 + (modifiedDifferentialDelta / timeDeltaIn(simulationResult.startDate, simulationResult.endDate, ChronoUnit.DAYS)),
                Math.pow(simulationResult.tradeCount, TRADE_COUNT_DIMINISHING_POWER))
    }

    /**
     * Get time delta
     *
     * @param from
     * @param to
     * @param unit
     * @return
     */
    static Double timeDeltaIn(Date from, Date to, ChronoUnit unit) {
        Double diff = (from && to) ? Math.abs(to.time - from.time) : 0
        switch (unit) {
            case ChronoUnit.DAYS:
                return diff / 86400000
            case ChronoUnit.HOURS:
                return diff / 3600000
            case ChronoUnit.MINUTES:
                return diff / 60000
            case ChronoUnit.SECONDS:
                return diff / 1000
            case ChronoUnit.MILLIS:
                return diff
        }
        return null
    }

}
