package com.ukora.tradestudent.services

import com.ukora.tradestudent.entities.AbstractAssociation
import com.ukora.tradestudent.entities.Brain
import com.ukora.tradestudent.entities.Lesson
import com.ukora.tradestudent.entities.Memory
import com.ukora.tradestudent.utils.NerdUtils
import groovy.util.logging.Log4j2
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct

@Log4j2
@Service
class CaptureAssociationsService {

    public static final String OBNOXIOUS_REFERENCE_SEPARATOR = "/"
    public static final String GENERAL_ASSOCIATION_REFERENCE = "general"
    public static final String INSTANT_TIME_REFERENCE = "instant"
    public static final String PRICE_DELTA = "priceDelta"

    public static boolean leaningEnabled = false
    public static Integer learningSpeed = 1

    @Autowired
    BytesFetcherService bytesFetcherService

    @PostConstruct
    init(){
        //bytesFetcherService.resetLessons()
        //bytesFetcherService.wiskyBender()
    }

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

                    /** Hydrate assocation tags */
                    hydrateAssociationTags(lesson, lesson.getTag().getTagName())

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
     * Hydrate the association tags
     *
     * @param associations
     * @param tagName
     */
    void hydrateAssociationTags(AbstractAssociation associations, String tagName){

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
    void brainItUp(Memory memory, AbstractAssociation lesson, String timeDelta, String tagName){
        if(!memory) return
        if(!lesson) return
        /** Normalized properties */
        memory.normalized.properties.each { prop, val ->
            if(val instanceof Double) {
                (lesson.associations.get(timeDelta + OBNOXIOUS_REFERENCE_SEPARATOR + prop, [:]) as Map).put(tagName, Double.parseDouble(val as String))
                (lesson.associations.get(timeDelta + OBNOXIOUS_REFERENCE_SEPARATOR + prop, [:]) as Map).put(GENERAL_ASSOCIATION_REFERENCE, Double.parseDouble(val as String))
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
        brain.standard_deviation = NerdUtils.applyValueGetNewDeviation(
            value,
            brain.mean,
            brain.count + 1,
            brain.standard_deviation)
        brain.mean = NerdUtils.applyValueGetNewMean(
            value,
            brain.mean,
            brain.count)
        brain.count++
        bytesFetcherService.saveBrain(brain)
    }

}
