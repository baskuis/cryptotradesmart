package com.ukora.tradestudent.services

import com.ukora.tradestudent.entities.*
import com.ukora.tradestudent.utils.NerdUtils
import groovy.util.logging.Log4j2
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Log4j2
@Service
class CaptureAssociationsService {

    public static final String OBNOXIOUS_REFERENCE_SEPARATOR = "/"
    public static final String GENERAL_ASSOCIATION_REFERENCE = "general"
    public static final String INSTANT_TIME_REFERENCE = "instant"
    public static final String PRICE_DELTA = "priceDelta"

    public static final String TIME_HOUR_IN_DAY = 'hourinday'
    public static final String TIME_MINUTE_IN_HOUR = 'minuteinhour'
    public static final String TIME_DAY_IN_WEEK = 'dayinweek'
    public static final String TIME_DAY_IN_MONTH = 'dayinmonth'

    public static boolean leaningEnabled = false
    public static Integer learningSpeed = 1

    @Autowired
    BytesFetcherService bytesFetcherService

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
                println String.format("capturing lesson %s", it)
                Lesson lesson = bytesFetcherService.getNextLesson()
                if (lesson) {

                    /** Hydrate lesson */
                    bytesFetcherService.hydrateAssociation(lesson)

                    /** Hydrate association tags */
                    hydrateAssociationTags(lesson, lesson.getTag().getTagName())

                    /** Hydrate specialized numeric association tags */
                    hydrateSpecializedAssociationTags(lesson, lesson.getTag().getTagName())

                    /** Remember all that */
                    rememberAllThat(lesson)

                    println "done"
                } else {
                    println "no lesson"
                }
            }
        }else{
            println "leaning disabled"
        }
    }

    /**
     * Hydrate numeric associations for a moment in time
     *
     * @param correlationAssociation
     */
    void hydrateAssocations(CorrelationAssociation correlationAssociation){

        /** Capture associations for normalized data for instance */
        brainItUpSimple(correlationAssociation.memory, correlationAssociation, INSTANT_TIME_REFERENCE)

        /** Capture associations for normalized data at other time deltas */
        correlationAssociation.intervals.each { String timeDelta ->
            Memory memory = correlationAssociation.previousMemory.get(timeDelta)
            if(memory){ brainItUpSimple(memory, correlationAssociation, timeDelta) }
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
                correlationAssociation.numericAssociations.put(timeDelta + OBNOXIOUS_REFERENCE_SEPARATOR + prop, val)
            }
        }

        /** Previous price proportions */
        Double previousPriceProportion = correlationAssociation.previousPrices.get(timeDelta)
        if(previousPriceProportion && !previousPriceProportion.naN) {
            correlationAssociation.numericAssociations.put(timeDelta + OBNOXIOUS_REFERENCE_SEPARATOR + PRICE_DELTA, previousPriceProportion)
        }

    }

    /**
     * Hydrate other specialized numeric associations
     *
     * @param associations
     * @param tagName
     */
    static void hydrateSpecializedAssociationTags(AbstractAssociation associations, String tagName){

        if(!associations) return
        if(!tagName) return
        if(!associations.date) return

        /**
         * Store time based associations
         */
        Calendar calendar = GregorianCalendar.getInstance()
        calendar.setTime(associations.date)
        associations.associations.get(INSTANT_TIME_REFERENCE + OBNOXIOUS_REFERENCE_SEPARATOR + TIME_HOUR_IN_DAY, [:]).put(tagName, calendar.get(Calendar.HOUR_OF_DAY))
        associations.associations.get(INSTANT_TIME_REFERENCE + OBNOXIOUS_REFERENCE_SEPARATOR + TIME_HOUR_IN_DAY, [:]).put(GENERAL_ASSOCIATION_REFERENCE, calendar.get(Calendar.HOUR_OF_DAY))
        associations.associations.get(INSTANT_TIME_REFERENCE + OBNOXIOUS_REFERENCE_SEPARATOR + TIME_MINUTE_IN_HOUR, [:]).put(tagName, calendar.get(Calendar.MINUTE))
        associations.associations.get(INSTANT_TIME_REFERENCE + OBNOXIOUS_REFERENCE_SEPARATOR + TIME_MINUTE_IN_HOUR, [:]).put(GENERAL_ASSOCIATION_REFERENCE, calendar.get(Calendar.MINUTE))
        associations.associations.get(INSTANT_TIME_REFERENCE + OBNOXIOUS_REFERENCE_SEPARATOR + TIME_DAY_IN_WEEK, [:]).put(tagName, calendar.get(Calendar.DAY_OF_WEEK))
        associations.associations.get(INSTANT_TIME_REFERENCE + OBNOXIOUS_REFERENCE_SEPARATOR + TIME_DAY_IN_WEEK, [:]).put(GENERAL_ASSOCIATION_REFERENCE, calendar.get(Calendar.DAY_OF_WEEK))
        associations.associations.get(INSTANT_TIME_REFERENCE + OBNOXIOUS_REFERENCE_SEPARATOR + TIME_DAY_IN_MONTH, [:]).put(tagName, calendar.get(Calendar.DAY_OF_MONTH))
        associations.associations.get(INSTANT_TIME_REFERENCE + OBNOXIOUS_REFERENCE_SEPARATOR + TIME_DAY_IN_MONTH, [:]).put(GENERAL_ASSOCIATION_REFERENCE, calendar.get(Calendar.DAY_OF_MONTH))

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
        brainItUp(associations.memory, associations, INSTANT_TIME_REFERENCE, tagName)

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
                (lesson.associations.get(timeDelta + OBNOXIOUS_REFERENCE_SEPARATOR + prop, [:]) as Map).put(tagName, val)
                (lesson.associations.get(timeDelta + OBNOXIOUS_REFERENCE_SEPARATOR + prop, [:]) as Map).put(GENERAL_ASSOCIATION_REFERENCE, val)
            }
        }

        /** Previous price proportions */
        Double previousPriceProportion = lesson.previousPrices.get(timeDelta)
        if(previousPriceProportion) {
            (lesson.associations.get(timeDelta + OBNOXIOUS_REFERENCE_SEPARATOR + PRICE_DELTA, [:]) as Map).put(tagName, previousPriceProportion)
            (lesson.associations.get(timeDelta + OBNOXIOUS_REFERENCE_SEPARATOR + PRICE_DELTA, [:]) as Map).put(GENERAL_ASSOCIATION_REFERENCE, previousPriceProportion)
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
            println String.format("Not capturing new value mean %s deviation %s", newMean, newDeviation)
            return
        }
        brain.standard_deviation = newDeviation
        brain.mean = newMean
        brain.count++
        bytesFetcherService.saveBrain(brain)
    }

}
