package com.ukora.tradestudent.services.graphs

import com.ukora.domain.entities.CorrelationAssociation
import com.ukora.domain.entities.ExtractedText
import com.ukora.domain.entities.TextCorrelationAssociation
import com.ukora.domain.repositories.CorrelationAssociationRepository
import com.ukora.domain.repositories.TextCorrelationAssociationRepository
import com.ukora.tradestudent.services.SimulationResultService
import com.ukora.tradestudent.services.TagService
import com.ukora.tradestudent.utils.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

import java.time.Duration
import java.time.Instant

@Service
class GraphDataService {

    static final Double HALF = 0.5

    static final enum Range {
        DAILY, WEEKLY, MONTHLY
    }

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
    }

    static class DataCapture {
        TextCorrelationAssociation textCorrelationAssociation
        CorrelationAssociation correlationAssociation
    }

    public static List<DataPoint> DataPoints = []
    static TreeMap<Date, DataCapture> DataCaptures = []

    static List<List> getRange(Range range) {
        long timeDiff
        int numberOfMinutes
        switch (range) {
            case Range.DAILY:
                timeDiff = 86400000
                numberOfMinutes = 10
                break
            case Range.WEEKLY:
                timeDiff = 7 * 86400000
                numberOfMinutes = 30
                break
            case Range.MONTHLY:
                timeDiff = 30 * 86400000
                numberOfMinutes = 120
                break
            default:
                Logger.log('Invalid range passed')
                return []
        }
        List filtered = DataPoints.findAll {
            it.date.time >= Date.newInstance().time - timeDiff
        }
        def result = []
        int cur = 0
        while (cur < filtered.size()) {
            def last = (cur + numberOfMinutes < filtered.size()) ? cur + numberOfMinutes : filtered.size() - 1
            def chunk = filtered[cur..last]
            def lowestPrice = chunk.min { DataPoint dataPoint -> dataPoint.price }.price
            def highestPrice = chunk.max { DataPoint dataPoint -> dataPoint.price }.price
            def middlePrice = (lowestPrice + highestPrice) / 2
            def avgNumerical = chunk.sum { DataPoint dataPoint -> dataPoint.numericalProbability } / (last - cur)
            def avgTwitter = chunk.sum { DataPoint dataPoint -> dataPoint.textTwitterProbability } / (last - cur)
            def avgNews = chunk.sum { DataPoint dataPoint -> dataPoint.textNewsProbability } / (last - cur)
            def avgCombined = chunk.sum { DataPoint dataPoint -> dataPoint.combinedProbability } / (last - cur)
            result << [
                    filtered[cur].date,
                    middlePrice,
                    avgNumerical,
                    avgTwitter,
                    avgNews,
                    avgCombined
            ]
            cur += numberOfMinutes
        }
        return result
    }

    def collect() {
        Calendar cal = Calendar.getInstance()
        cal.setTime(new Date())
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        Instant end = Instant.now()
        Duration gap = Duration.ofSeconds(60)
        Instant current = cal.toInstant().minusSeconds(10l * 84600l)
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
                Logger.log('Unable to retrieve textCorrelations')
                continue
            }
            if (!correlationAssociations || correlationAssociations.size() == 0) {
                Logger.log('Unable to retrieve correlations')
                continue
            }
            DataCaptures.put(Date.from(current), new DataCapture(
                    correlationAssociation: correlationAssociations?.first(),
                    textCorrelationAssociation: textCorrelationAssociations?.first()
            ))
        }
    }

    @Scheduled(cron = '0 */3 * * * *')
    def generate() {

        /** Collect captures */
        collect()

        /** Get top performing simulations */
        def numericalSimulation = simulationResultService.getTopPerformingNumericalFlexSimulation()
        def textNewsSimulation = simulationResultService.getTopPerformingTextFlexSimulation(ExtractedText.TextSource.NEWS)
        def textTwitterSimulation = simulationResultService.getTopPerformingTextFlexSimulation(ExtractedText.TextSource.TWITTER)
        def combinedSimulation = simulationResultService.getTopPerformingCombinedSimulation()

        /** Get total tag weights for combined */
        def totalNumericalWeights = combinedSimulation.numericalSimulation.tagGroupWeights.values().collect({
            return Math.abs(it)
        }).sum() as float
        def totalTwitterWeights = combinedSimulation.textTwitterSimulation.tagGroupWeights.values().collect({
            return Math.abs(it)
        }).sum() as float
        def totalNewsWeights = combinedSimulation.textTwitterSimulation.tagGroupWeights.values().collect({
            return Math.abs(it)
        }).sum() as float

        /** Create the graphing data sets */
        DataPoints = DataCaptures.collect({
            DataCapture dataCapture = it.value

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

            Logger.log(String.format('price: %s, caDate: %s, taDate: %s, numericalAggregate: %s, textTwitterAggregate: %s, textNewsAggregate: %s, totalAggregate: %s, totalNumericalWeights: %s, totalTwitterWeights: %s, totalNewsWeights: %s',
                    dataCapture.correlationAssociation.price,
                    dataCapture.correlationAssociation.date,
                    dataCapture.textCorrelationAssociation.date,
                    numericalAggregate,
                    textTwitterAggregate,
                    textNewsAggregate,
                    totalAggregate,
                    totalNumericalWeights,
                    totalTwitterWeights,
                    totalNewsWeights
            ))

            return new DataPoint(
                    price: dataCapture.correlationAssociation.price,
                    date: dataCapture.correlationAssociation.date,
                    numericalProbability: numericalAggregate,
                    textNewsProbability: textTwitterAggregate,
                    textTwitterProbability: textNewsAggregate,
                    combinedProbability: totalAggregate
            )

        })

    }

}
