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
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class GraphDataService {

    static final int SET_BACK_SECONDS = 30 * 60
    static final int HISTORICAL_INTERVAL = 10 * 60
    static final Double HALF = 0.5
    static final int RETRIEVE_DATA_POINTS = 10000
    static final String SORT_FIELD = 'date'

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

    static List<DataPoint> DataPoints = []

    static boolean matchesDateApproximately(Date a, Date b) {
        long al = a.time - 45000
        long ah = a.time + 45000
        return al < b.time && b.time < ah
    }

    @Scheduled(cron = '0 * * * * *')
    def generate() {

        /** Get top performing simulations */
        def numericalSimulation = simulationResultService.getTopPerformingNumericalFlexSimulation()
        def textNewsSimulation = simulationResultService.getTopPerformingTextFlexSimulation(ExtractedText.TextSource.NEWS)
        def textTwitterSimulation = simulationResultService.getTopPerformingTextFlexSimulation(ExtractedText.TextSource.TWITTER)
        def combinedSimulation = simulationResultService.getTopPerformingCombinedSimulation()

        Page<TextCorrelationAssociation> textCorrelations = null
        Page<CorrelationAssociation> correlations = null
        List<Thread> collectors = []

        /** Get last N text correlations */
        collectors.push(Thread.start {
            textCorrelations = textCorrelationAssociationRepository.findAll(
                    new PageRequest(0, RETRIEVE_DATA_POINTS, new Sort(
                            Sort.Direction.DESC, SORT_FIELD
                    ))
            )
        })

        /** Get last N correlations */
        collectors.push(Thread.start {
            correlations = correlationAssociationRepository.findAll(
                    new PageRequest(0, RETRIEVE_DATA_POINTS, new Sort(
                            Sort.Direction.DESC, SORT_FIELD
                    ))
            )
        })

        collectors*.join()

        if(!textCorrelations || !textCorrelations.size) {
            Logger.log('Unable to retrieve textCorrelations')
            return
        }
        if(!correlations || !correlations.size) {
            Logger.log('Unable to retrieve correlations')
            return
        }

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
        DataPoints = textCorrelations.collect({
            TextCorrelationAssociation textCorrelationAssociation ->

                /** Assemble both correlation association and text correlation association */
                CorrelationAssociation correlationAssociation = correlations.find({
                    return matchesDateApproximately(it.date, textCorrelationAssociation.date)
                }) as CorrelationAssociation

                if (!correlationAssociation) {
                    Logger.log(String.format('Unable to find correlationAssociation for date: %s', textCorrelationAssociation.date))
                    return null
                }

                /** Get numerical aggregate */
                def numericalAggregate = combinedSimulation.numericalSimulation.tagGroupWeights.collect({
                    return it.value * (correlationAssociation.tagProbabilities[combinedSimulation.numericalSimulation.probabilityCombinerStrategy].get(
                            tagService.getTagsByTagGroupName(it.key).find({ it.entry() }).tagName
                    ) - HALF)
                }).sum() / totalNumericalWeights

                /** Get text twitter aggregate */
                def textTwitterAggregate = combinedSimulation.textTwitterSimulation.tagGroupWeights.collect({
                    return it.value * (textCorrelationAssociation.strategyProbabilities['weightedTextProbabilityCombinerStrategy'].get(
                            ExtractedText.TextSource.TWITTER as String
                    ).get(
                            tagService.getTagsByTagGroupName(it.key).find({ it.entry() }).tagName
                    ) - HALF)
                }).sum() / totalTwitterWeights

                /** Get text news aggregate */
                def textNewsAggregate = combinedSimulation.textTwitterSimulation.tagGroupWeights.collect({
                    return it.value * (textCorrelationAssociation.strategyProbabilities['weightedTextProbabilityCombinerStrategy'].get(
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

                Logger.debug(String.format('price: %s, caDate: %s, taDate: %s, numericalAggregate: %s, textTwitterAggregate: %s, textNewsAggregate: %s, totalAggregate: %s, totalNumericalWeights: %s, totalTwitterWeights: %s, totalNewsWeights: %s',
                        correlationAssociation.price,
                        correlationAssociation.date,
                        textCorrelationAssociation.date,
                        numericalAggregate,
                        textTwitterAggregate,
                        textNewsAggregate,
                        totalAggregate,
                        totalNumericalWeights,
                        totalTwitterWeights,
                        totalNewsWeights
                ))

                return new DataPoint(
                        price: correlationAssociation.price,
                        date: correlationAssociation.date,
                        numericalProbability: numericalAggregate,
                        textNewsProbability: textTwitterAggregate,
                        textTwitterProbability: textNewsAggregate,
                        combinedProbability: totalAggregate
                )

        })

    }

}
