package com.ukora.tradestudent.services.graphs

import com.ukora.domain.entities.CorrelationAssociation
import com.ukora.domain.entities.ExtractedText
import com.ukora.domain.entities.SimulationResult
import com.ukora.domain.entities.TextCorrelationAssociation
import com.ukora.domain.repositories.CorrelationAssociationRepository
import com.ukora.domain.repositories.TextCorrelationAssociationRepository
import com.ukora.tradestudent.services.SimulationResultService
import com.ukora.tradestudent.services.TagService
import com.ukora.tradestudent.utils.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

import java.time.Duration
import java.time.Instant

@Service
class GraphDataService {

    static final Double HALF = 0.5

    static final long GET_NUMBER_OF_DAYS = 40l

    static final enum Range {
        DAILY, WEEKLY, MONTHLY, MAX
    }

    SimulationResult numericalSimulation
    SimulationResult textNewsSimulation
    SimulationResult textTwitterSimulation
    SimulationResult combinedSimulation

    float singleTotalNumericalWeights
    float singleTotalTwitterWeights
    float singleTotalNewsWeights
    float totalNumericalWeights
    float totalTwitterWeights
    float totalNewsWeights

    @Autowired
    TextCorrelationAssociationRepository textCorrelationAssociationRepository

    @Autowired
    CorrelationAssociationRepository correlationAssociationRepository

    @Autowired
    SimulationResultService simulationResultService

    @Autowired
    TagService tagService

    static class DataPoint {
        float price
        Date date
        float numericalProbability
        float textNewsProbability
        float textTwitterProbability
        float combinedProbability
        float singleNumericalProbability
        float singleTextNewsProbability
        float singleTextTwitterProbability
    }

    static class DataCapture {
        TextCorrelationAssociation textCorrelationAssociation
        CorrelationAssociation correlationAssociation
    }

    public static List<DataPoint> DataPoints = []
    static SortedMap<Long, DataCapture> DataCaptures = Collections.synchronizedSortedMap(new TreeMap<Long, DataCapture>())

    static List<List> getRange(Range range) {
        while (LOCKED) {
            Logger.log('getRange locked, waiting')
            sleep(100)
        }
        long timeDiff
        int numberOfMinutes
        switch (range) {
            case Range.DAILY:
                timeDiff = 86400000l
                numberOfMinutes = 5
                break
            case Range.WEEKLY:
                timeDiff = 7l * 86400000l
                numberOfMinutes = 10
                break
            case Range.MONTHLY:
                timeDiff = 30l * 86400000l
                numberOfMinutes = 30
                break
            case Range.MAX:
                timeDiff = 45l * 86400000l
                numberOfMinutes = 60
                break
            default:
                Logger.log('Invalid range passed')
                return []
        }
        def result = []
        try {
            Logger.log(String.format('Getting %s minute chunks from %s ms ago', numberOfMinutes, timeDiff))
            List filtered = DataPoints.findAll {
                it?.date?.time >= Date.newInstance().time - timeDiff
            }
            Logger.log(String.format('Find filtered[%s]', filtered?.size()))
            int cur = 0
            while (cur < filtered.size()) {
                def last = (cur + numberOfMinutes < filtered.size()) ? cur + numberOfMinutes : filtered.size() - 1
                def chunk = filtered[cur..last].findAll { DataPoint dataPoint ->
                    (
                            dataPoint &&
                                    dataPoint.price && dataPoint.price > 0 &&
                                    dataPoint.numericalProbability && dataPoint.numericalProbability != Float.NaN &&
                                    dataPoint.textTwitterProbability && dataPoint.textTwitterProbability != Float.NaN &&
                                    dataPoint.textNewsProbability && dataPoint.textNewsProbability != Float.NaN &&
                                    dataPoint.combinedProbability && dataPoint.combinedProbability != Float.NaN
                    )
                }
                def lowestPrice = chunk.min { DataPoint dataPoint -> dataPoint?.price }?.price
                def highestPrice = chunk.max { DataPoint dataPoint -> dataPoint?.price }?.price
                if (!lowestPrice || !highestPrice) {
                    Logger.log(String.format('No lowest or highest price found'))
                    if (result.size() > 0) {
                        result << result.last()
                    }
                } else {
                    def middlePrice = (lowestPrice + highestPrice) / 2

                    def avgNumerical = chunk.sum { DataPoint dataPoint -> dataPoint?.numericalProbability } / chunk.size()
                    def avgTwitter = chunk.sum { DataPoint dataPoint -> dataPoint?.textTwitterProbability } / chunk.size()
                    def avgNews = chunk.sum { DataPoint dataPoint -> dataPoint?.textNewsProbability } / chunk.size()

                    def avgCombined = chunk.sum { DataPoint dataPoint -> dataPoint?.combinedProbability } / chunk.size()

                    def avgSingleNumerical = chunk.sum { DataPoint dataPoint -> dataPoint?.singleNumericalProbability } / chunk.size()
                    def avgSingleTwitter = chunk.sum { DataPoint dataPoint -> dataPoint?.singleTextTwitterProbability } / chunk.size()
                    def avgSingleNews = chunk.sum { DataPoint dataPoint -> dataPoint?.singleTextNewsProbability } / chunk.size()

                    result << [
                            filtered[cur].date,
                            middlePrice,
                            avgNumerical,
                            avgTwitter,
                            avgNews,
                            avgCombined,
                            avgSingleNumerical,
                            avgSingleNews,
                            avgSingleTwitter,
                    ]

                }
                cur += numberOfMinutes
            }
            return result
        } catch (Exception e) {
            Logger.log(e.message)
            e.printStackTrace()
        }
        return null
    }

    def collect() {
        Calendar cal = Calendar.getInstance()
        cal.setTime(new Date())
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        Instant end = Instant.now()
        Duration gap = Duration.ofSeconds(60)
        Instant current = cal.toInstant().minusSeconds(GET_NUMBER_OF_DAYS * 84600l)
        while (current.isBefore(end)) {
            end = Instant.now()
            current = current + gap
            List<TextCorrelationAssociation> textCorrelationAssociations = textCorrelationAssociationRepository.findByDateBetween(
                    Date.from(current.minusSeconds(45)),
                    Date.from(current.plusSeconds(45))
            )
            List<CorrelationAssociation> correlationAssociations = correlationAssociationRepository.findByDateBetween(
                    Date.from(current.minusSeconds(45)),
                    Date.from(current.plusSeconds(45))
            )
            if (!textCorrelationAssociations || textCorrelationAssociations.size() == 0) {
                Logger.log(String.format('Unable to retrieve textCorrelations for %s', current))
                continue
            }
            if (!correlationAssociations || correlationAssociations.size() == 0) {
                Logger.log(String.format('Unable to retrieve correlations for %s', current))
                continue
            }
            DataCaptures.put(Date.from(current).time, new DataCapture(
                    correlationAssociation: correlationAssociations?.first(),
                    textCorrelationAssociation: textCorrelationAssociations?.first()
            ))
        }
    }

    /**
     *
     * 1.) Build initial reference (updated when simulations run)
     * 2.) Add new entries to reference (updated on live trading)
     * 3.) Stuff..
     *
     *
     */
    static synchronized boolean LOCKED = false

    @Scheduled(initialDelay = 5000l, fixedRate = 3600000l)
    @Async
    void addAll() {
        if(LOCKED) {
            Logger.log('Locked not running addAll')
            return
        }
        try {
            LOCKED = true
            println '========= addAll =========='
            println String.format('DataCaptures.size() = %s', DataCaptures.size())
            println String.format('DataPoints.size() = %s', DataPoints.size())
            collect()
            generate(false)
        } catch (Exception e) {
            Logger.log('Unable to run addAll. Message: ' + e.message)
            e.printStackTrace()
        } finally {
            LOCKED = false
            println '========= done =========='
            println String.format('DataCaptures.size() = %s', DataCaptures.size())
            println String.format('DataPoints.size() = %s', DataPoints.size())
            println '========= done =========='
        }
    }

    @Scheduled(cron = '10 */2 * * * *')
    @Async
    void addOnly() {
        if(LOCKED) {
            Logger.log('Locked not running addOnly')
            return
        }
        try {
            LOCKED = true
            println '========= addOnly =========='
            println String.format('DataCaptures.size() = %s', DataCaptures.size())
            println String.format('DataPoints.size() = %s', DataPoints.size())
            collectNew()
            generate(true)
        } catch (Exception e) {
            Logger.log('Unable to run addOnly. Message:' + e.message)
            e.printStackTrace()
        } finally {
            LOCKED = false
            println '========= done =========='
            println String.format('DataCaptures.size() = %s', DataCaptures.size())
            println String.format('DataPoints.size() = %s', DataPoints.size())
            println '========= done =========='
        }
    }

    def collectNew() {
        Calendar cal = Calendar.getInstance()
        cal.setTime(new Date())
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        Instant end = Instant.now()
        Duration gap = Duration.ofSeconds(60)
        Instant current = cal.toInstant().minusSeconds(3600)
        while (current.isBefore(end)) {
            end = Instant.now()
            current = current + gap
            if (!DataCaptures.get(Date.from(current).time)) {
                List<TextCorrelationAssociation> textCorrelationAssociations = textCorrelationAssociationRepository.findByDateBetween(
                        Date.from(current.minusSeconds(45)),
                        Date.from(current.plusSeconds(45))
                )
                List<CorrelationAssociation> correlationAssociations = correlationAssociationRepository.findByDateBetween(
                        Date.from(current.minusSeconds(45)),
                        Date.from(current.plusSeconds(45))
                )
                if (!textCorrelationAssociations || textCorrelationAssociations.size() == 0) {
                    Logger.log(String.format('Unable to retrieve textCorrelations for %s', current))
                    continue
                }
                if (!correlationAssociations || correlationAssociations.size() == 0) {
                    Logger.log(String.format('Unable to retrieve correlations for %s', current))
                    continue
                }
                DataCaptures.put(Date.from(current).time, new DataCapture(
                        correlationAssociation: correlationAssociations?.first(),
                        textCorrelationAssociation: textCorrelationAssociations?.first()
                ))
            }
        }


    }

    @Scheduled(initialDelay = 3000l, fixedRate = 600000l)
    synchronized setSimulations() {

        Logger.log('Setting simulations')

        /** Get top performing simulations */
        numericalSimulation = simulationResultService.getTopPerformingNumericalFlexSimulation()
        textNewsSimulation = simulationResultService.getTopPerformingTextFlexSimulation(ExtractedText.TextSource.NEWS)
        textTwitterSimulation = simulationResultService.getTopPerformingTextFlexSimulation(ExtractedText.TextSource.TWITTER)
        combinedSimulation = simulationResultService.getTopPerformingCombinedSimulation()

        /** Get numeric simulation weights */
        singleTotalNumericalWeights = numericalSimulation.tagGroupWeights.values().collect({
            return Math.abs(it)
        }).sum() as float
        /** Get text twitter weights */
        singleTotalTwitterWeights = textNewsSimulation.tagGroupWeights.values().collect({
            return Math.abs(it)
        }).sum() as float
        /** Get text news weights */
        singleTotalNewsWeights = textTwitterSimulation.tagGroupWeights.values().collect({
            return Math.abs(it)
        }).sum() as float

        /** Get total tag weights for combined */
        totalNumericalWeights = combinedSimulation.numericalSimulation.tagGroupWeights.values().collect({
            return Math.abs(it)
        }).sum() as float
        totalTwitterWeights = combinedSimulation.textTwitterSimulation.tagGroupWeights.values().collect({
            return Math.abs(it)
        }).sum() as float
        totalNewsWeights = combinedSimulation.textTwitterSimulation.tagGroupWeights.values().collect({
            return Math.abs(it)
        }).sum() as float

    }

    def assureSimulations() {
        if (!numericalSimulation || !textNewsSimulation || !textTwitterSimulation || !combinedSimulation) {
            setSimulations()
        }
    }

    def generate(boolean append) {

        Logger.log('Running generate data points')

        /** Assure simulation are set */
        assureSimulations()

        /** Create the graphing data sets - rebuild it */
        if (!append) {

            Logger.log('Building new list of data points')
            DataPoints = DataCaptures.collect({
                DataCapture dataCapture = it.value
                return buildDataPoint(
                        dataCapture,
                        new Date(it.key)
                )
            })
            Logger.log('Done')

            /** Or only add new ones */
        } else {

            Logger.log('Appending to list of data points')
            List add = []
            long yesterday = yesterday()
            DataCaptures.each {
                long timestamp = it.key
                DataCapture dataCapture = it.value
                if (timestamp > yesterday) {
                    if (!DataPoints.find {
                        it.date.time == timestamp
                    }) {
                        add.push(
                                buildDataPoint(
                                        dataCapture,
                                        new Date(timestamp)
                                )
                        )
                    }
                }
            }
            DataPoints.addAll(add)
            Logger.log('Done')

        }

        Logger.log('Done generating data points')

    }

    static long yesterday() {
        final Calendar cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -1)
        return cal.getTime().time
    }

    DataPoint buildDataPoint(DataCapture dataCapture, Date date) {

        /** Get single numerical aggregate */
        def singleNumericalAggregate = numericalSimulation.tagGroupWeights.collect({
            return it.value * (dataCapture.correlationAssociation.tagProbabilities[combinedSimulation.numericalSimulation.probabilityCombinerStrategy].get(
                    tagService.getTagsByTagGroupName(it.key).find({ it.entry() }).tagName
            ) - HALF)
        }).sum() / singleTotalNumericalWeights

        /** Get single text twitter aggregate */
        def singleTextTwitterAggregate = textTwitterSimulation.tagGroupWeights.collect({
            return it.value * (dataCapture.textCorrelationAssociation.strategyProbabilities['weightedTextProbabilityCombinerStrategy'].get(
                    ExtractedText.TextSource.TWITTER as String
            ).get(
                    tagService.getTagsByTagGroupName(it.key).find({ it.entry() }).tagName
            ) - HALF)
        }).sum() / singleTotalTwitterWeights

        /** Get single text news aggregate */
        def singleTextNewsAggregate = textNewsSimulation.tagGroupWeights.collect({
            return it.value * (dataCapture.textCorrelationAssociation.strategyProbabilities['weightedTextProbabilityCombinerStrategy'].get(
                    ExtractedText.TextSource.NEWS as String
            ).get(
                    tagService.getTagsByTagGroupName(it.key).find({ it.entry() }).tagName
            ) - HALF)
        }).sum() / singleTotalNewsWeights

        /** Get numerical aggregate */
        def numericalAggregate = combinedSimulation.numericalSimulation.tagGroupWeights.collect({
            return it.value * (dataCapture.correlationAssociation.tagProbabilities[combinedSimulation.numericalSimulation.probabilityCombinerStrategy].get(
                    tagService.getTagsByTagGroupName(it.key).find({ it.entry() }).tagName
            ) - HALF)
        }).sum() / totalNumericalWeights

        /** Get text twitter aggregate */
        def textTwitterAggregate = combinedSimulation.textTwitterSimulation.tagGroupWeights.collect({
            return it.value * (dataCapture.textCorrelationAssociation.strategyProbabilities['weightedTextProbabilityCombinerStrategy'].get(
                    ExtractedText.TextSource.TWITTER as String
            ).get(
                    tagService.getTagsByTagGroupName(it.key).find({ it.entry() }).tagName
            ) - HALF)
        }).sum() / totalTwitterWeights

        /** Get text news aggregate */
        def textNewsAggregate = combinedSimulation.textTwitterSimulation.tagGroupWeights.collect({
            return it.value * (dataCapture.textCorrelationAssociation.strategyProbabilities['weightedTextProbabilityCombinerStrategy'].get(
                    ExtractedText.TextSource.NEWS as String
            ).get(
                    tagService.getTagsByTagGroupName(it.key).find({ it.entry() }).tagName
            ) - HALF)
        }).sum() / totalNewsWeights

        /** Get total aggregate */
        def totalAggregate = HALF + (
                (
                        (combinedSimulation.numericalWeight * numericalAggregate) +
                                (combinedSimulation.textNewsWeight * textTwitterAggregate) +
                                (combinedSimulation.textTwitterWeight * textNewsAggregate)
                ) / (
                        Math.abs(combinedSimulation.numericalWeight) +
                                Math.abs(combinedSimulation.textNewsWeight) +
                                Math.abs(combinedSimulation.textTwitterWeight)
                )
        )

        Logger.debug(String.format('price: %s, caDate: %s, taDate: %s, numericalAggregate: %s, textTwitterAggregate: %s, textNewsAggregate: %s, singleNumericalAggregate: %s, singleTextTwitterAggregate: %s, singleTextNewsAggregate: %s, totalAggregate: %s, totalNumericalWeights: %s, totalTwitterWeights: %s, totalNewsWeights: %s',
                dataCapture.correlationAssociation.price,
                dataCapture.correlationAssociation.date,
                dataCapture.textCorrelationAssociation.date,
                numericalAggregate,
                textTwitterAggregate,
                textNewsAggregate,
                singleNumericalAggregate,
                singleTextTwitterAggregate,
                singleTextNewsAggregate,
                totalAggregate,
                totalNumericalWeights,
                totalTwitterWeights,
                totalNewsWeights
        ))

        return new DataPoint(
                price: dataCapture.correlationAssociation.price,
                date: date,
                numericalProbability: numericalAggregate,
                textNewsProbability: textTwitterAggregate,
                textTwitterProbability: textNewsAggregate,
                combinedProbability: totalAggregate,
                singleNumericalProbability: singleNumericalAggregate,
                singleTextTwitterProbability: singleTextTwitterAggregate,
                singleTextNewsProbability: singleTextNewsAggregate
        )
    }

}
