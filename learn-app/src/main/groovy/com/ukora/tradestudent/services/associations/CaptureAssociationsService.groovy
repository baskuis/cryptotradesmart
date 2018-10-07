package com.ukora.tradestudent.services.associations

import com.ukora.tradestudent.entities.*
import com.ukora.tradestudent.services.BytesFetcherService
import com.ukora.tradestudent.services.associations.text.CaptureTextAssociationsService
import com.ukora.tradestudent.utils.Logger
import com.ukora.tradestudent.utils.NerdUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class CaptureAssociationsService {

    public static final String SEP = "/"
    public static final String GENERAL = "general"
    public static final String INSTANT = "instant"
    public static final String PRICE_DELTA = "priceDelta"

    public static final String TIME_HOUR_IN_DAY = 'hourinday'
    public static final String TIME_MINUTE_IN_HOUR = 'minuteinhour'
    public static final String TIME_DAY_IN_WEEK = 'dayinweek'
    public static final String TIME_DAY_IN_MONTH = 'dayinmonth'

    public static boolean leaningEnabled = true
    public static Integer learningSpeed = 15

    public static Double PRACTICAL_ZERO = 0.000000001

    @Autowired
    BytesFetcherService bytesFetcherService

    @Autowired
    TechnicalAnalysisService technicalAnalysisService

    @Autowired
    CaptureTextAssociationsService captureTextAssociationsService

    /**
     * Main schedule to digest lessons
     * and create associations
     *
     */
    @Scheduled(cron = "*/10 * * * * *")
    @Async
    void learn(){
        if(leaningEnabled) {
            learningSpeed.times {
                Logger.debug(String.format("capturing lesson %s", it))
                Lesson lesson = bytesFetcherService.getNextLesson()
                if (lesson) {

                    /** Hydrate lesson */
                    bytesFetcherService.hydrateAssociation(lesson)

                    /** Hydrate association tags */
                    hydrateAssociationTags(lesson, lesson.tag?.tagName)

                    /** Hydrate specialized numeric association tags */
                    hydrateSpecializedAssociationTags(lesson, lesson.tag?.tagName)

                    /** Remember all that */
                    rememberAllThat(lesson)

                } else {
                    Logger.debug("no lesson")
                }
            }
        }else{
            Logger.log("leaning disabled")
        }
    }

    /**
     * Hydrate numeric associations for a moment in time
     *
     * @param correlationAssociation
     */
    void hydrateAssociations(CorrelationAssociation correlationAssociation){

        /** Capture associations for normalized data for instance */
        brainItUpSimple(correlationAssociation.memory, correlationAssociation, INSTANT)

        /** Capture associations for normalized data at other time deltas */
        correlationAssociation.intervals.each { String timeDelta ->
            Memory memory = correlationAssociation.previousMemory.get(timeDelta)
            if(memory){ brainItUpSimple(memory, correlationAssociation, timeDelta) }
        }

        /**
         * Store time based associations
         *
         */
        Calendar calendar = GregorianCalendar.getInstance()
        calendar.setTime(correlationAssociation.date)
        correlationAssociation.numericAssociations.put(INSTANT + SEP + TIME_HOUR_IN_DAY, calendar.get(Calendar.HOUR_OF_DAY) as Double)
        correlationAssociation.numericAssociations.put(INSTANT + SEP + TIME_MINUTE_IN_HOUR, calendar.get(Calendar.MINUTE) as Double)
        correlationAssociation.numericAssociations.put(INSTANT + SEP + TIME_DAY_IN_WEEK, calendar.get(Calendar.DAY_OF_WEEK) as Double)
        correlationAssociation.numericAssociations.put(INSTANT + SEP + TIME_DAY_IN_MONTH, calendar.get(Calendar.DAY_OF_MONTH) as Double)

        /**
         * Store emotional boundary associations
         *
         */
        if(correlationAssociation.price) {
            NerdUtils.extractBoundaryDistances(correlationAssociation.price).each {
                if(it.value == 0) it.value = PRACTICAL_ZERO
                correlationAssociation.numericAssociations.put(INSTANT + SEP + it.key, it.value)
            }
        }

        /**
         * Add technical analysis
         *
         */
        Map<String, Double> technicalAnalysis = technicalAnalysisService.extractTechnicalAnalysis(correlationAssociation.date)
        if(technicalAnalysis) {
            technicalAnalysis.each {
                correlationAssociation.numericAssociations.put(it.key, it.value)
            }
        }

    }

    /**
     * Hydrate numeric associations for a certain time delta
     *
     * @param memory
     * @param correlationAssociation
     * @param timeDelta
     */
    static void brainItUpSimple(Memory memory, CorrelationAssociation correlationAssociation, String timeDelta){

        if(!memory) return
        if(!correlationAssociation) return

        /** Normalized properties */
        memory.normalized.properties.each { prop, val ->
            if(val instanceof Double) {
                correlationAssociation.numericAssociations.put(timeDelta + SEP + prop, val)
            }
        }

        /** Previous price proportions */
        Double previousPriceProportion = correlationAssociation.previousPrices.get(timeDelta)
        if(previousPriceProportion && !previousPriceProportion.naN) {
            correlationAssociation.numericAssociations.put(timeDelta + SEP + PRICE_DELTA, previousPriceProportion)
        }

    }

    /**
     * Hydrate other specialized numeric associations
     *
     * @param associations
     * @param tagName
     */
    void hydrateSpecializedAssociationTags(AbstractAssociation associations, String tagName){

        if(!associations) return
        if(!tagName) return
        if(!associations.date) return

        /**
         * Store time based associations
         *
         */
        Calendar calendar = GregorianCalendar.getInstance()
        calendar.setTime(associations.date)
        associations.associations.get(INSTANT + SEP + TIME_HOUR_IN_DAY, [:]).put(tagName, calendar.get(Calendar.HOUR_OF_DAY))
        associations.associations.get(INSTANT + SEP + TIME_MINUTE_IN_HOUR, [:]).put(tagName, calendar.get(Calendar.MINUTE))
        associations.associations.get(INSTANT + SEP + TIME_DAY_IN_WEEK, [:]).put(tagName, calendar.get(Calendar.DAY_OF_WEEK))
        associations.associations.get(INSTANT + SEP + TIME_DAY_IN_MONTH, [:]).put(tagName, calendar.get(Calendar.DAY_OF_MONTH))

        /**
         * Store emotional boundary associations
         *
         */
        if(associations.price) {
            NerdUtils.extractBoundaryDistances(associations.price).each {
                if(it.value == 0) it.value = PRACTICAL_ZERO
                associations.associations.get(INSTANT + SEP + it.key, [:]).put(tagName, it.value)
            }
        }

        /**
         * Add technical analysis
         *
         */
        Map<String, Double> technicalAnalysis = technicalAnalysisService.extractTechnicalAnalysis(associations.date)
        if(technicalAnalysis) {
            technicalAnalysis.each {
                associations.associations.get(it.key, [:]).put(tagName, it.value)
            }
        }

    }

    /**
     * Hydrate the association tags
     *
     * @param associations
     * @param tagName
     */
    static void hydrateAssociationTags(AbstractAssociation associations, String tagName){

        if(!associations) return
        if(!tagName) return

        /** Capture associations for normalized data for instance */
        brainItUp(associations.memory, associations, INSTANT, tagName)

        /** Capture associations for normalized data at other time deltas */
        associations.intervals.each { String key ->
            Memory memory = associations.previousMemory.get(key)
            if(memory){ brainItUp(memory, associations, key, tagName) }
        }

    }

    /**
     * Brain it up - generate references - and update number associations
     *
     * @param memory
     * @param tagName
     * @param timeDelta
     */
    static void brainItUp(Memory memory, AbstractAssociation lesson, String timeDelta, String tagName){
        if(!memory) return
        if(!lesson) return

        /** Normalized properties */
        memory.normalized.properties.each { prop, val ->
            if(val instanceof Double) {
                (lesson.associations.get(timeDelta + SEP + prop, [:]) as Map).put(tagName, val)
            }
        }

        /** Previous price proportions */
        Double previousPriceProportion = lesson.previousPrices.get(timeDelta)
        if(previousPriceProportion) {
            (lesson.associations.get(timeDelta + SEP + PRICE_DELTA, [:]) as Map).put(tagName, previousPriceProportion)
        }

    }

    /**
     * Remember all that shit
     * Find existing number association - then add the new value
     * simple as that
     *
     * @param associations
     */
    void rememberAllThat(Lesson lesson){
        if(!lesson) return
        lesson.associations?.each {
            Map.Entry<String, Map<String, Double>> entry ->
            entry.value.each {
                Map.Entry<String, Double> tagEntry ->
                Brain brain = bytesFetcherService.getBrain(entry.key, tagEntry.key)
                captureNewValue(brain, tagEntry.value)
            }
        }
    }

    /**
     * Capture new value for number association
     *
     * @param brain
     * @param value
     */
    void captureNewValue(Brain brain, Double value){
        if(!value) return
        if(!brain) return
        Double newDeviation = NerdUtils.applyValueGetNewDeviation(
                value,
                brain.mean,
                brain.count + 1,
                brain.standard_deviation)
        Double newMean = NerdUtils.applyValueGetNewMean(
                value,
                brain.mean,
                brain.count)
        if(newDeviation == null || newDeviation.naN || newMean == null || newMean.naN){
            Logger.log(String.format("Not capturing new value mean %s deviation %s", newMean, newDeviation))
            return
        }
        brain.standard_deviation = newDeviation
        brain.mean = newMean
        brain.count++
        bytesFetcherService.saveBrain(brain)
    }

}
