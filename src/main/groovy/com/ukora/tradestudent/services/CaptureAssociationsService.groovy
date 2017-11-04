package com.ukora.tradestudent.services

import com.ukora.tradestudent.bayes.numbers.NumberAssociation
import com.ukora.tradestudent.entities.Lesson
import com.ukora.tradestudent.entities.Memory
import com.ukora.tradestudent.utils.NerdUtils
import groovy.util.logging.Log4j2
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct

@Log4j2
@Service
class CaptureAssociationsService {

    public static final String OBNOXIOUS_REFERENCE_SEPARATOR = "/"
    public static final String GENERAL_ASSOCIATION_REFERENCE = "general"
    public static final String INSTANT_TIME_REFERENCE = "instant"

    @Autowired
    BytesFetcherService bytesFetcherService

    @PostConstruct
    init(){
        //bytesFetcherService.resetLessons()
        //bytesFetcherService.wiskyBender()
    }

    @Scheduled(cron = "*/5 * * * * *")
    void learn(){
        println "learn.."
        Lesson lesson = bytesFetcherService.getNextLesson()
        if(lesson) {
            println "${lesson.tag} ${lesson.date}"
            try {
                lesson.memory = bytesFetcherService.getMemory(lesson.date)
                if(lesson.memory == null){
                    println "empty memory object"
                    return
                }
            } catch (Exception e) {
                e.printStackTrace()
            }
            try {
                lesson.twitter = bytesFetcherService.getTwitter(lesson.date)
            } catch (Exception e) {
                e.printStackTrace()
            }
            try {
                lesson.news = bytesFetcherService.getNews(lesson.date)
            } catch (Exception e) {
                e.printStackTrace()
            }
            lesson.intervals.each { String key ->
                Calendar calendar = Calendar.instance
                calendar.setTime(lesson.date)
                switch(key){
                    case '2minute':
                        calendar.add(Calendar.MINUTE, -2)
                        break
                    case '5minute':
                        calendar.add(Calendar.MINUTE, -5)
                        break
                    case '10minute':
                        calendar.add(Calendar.MINUTE, -10)
                        break
                    case '30minute':
                        calendar.add(Calendar.MINUTE, -30)
                        break
                    case '1hour':
                        calendar.add(Calendar.HOUR, -1)
                        break
                    case '2hour':
                        calendar.add(Calendar.HOUR, -2)
                        break
                    case '4hour':
                        calendar.add(Calendar.HOUR, -4)
                        break
                    case '8hour':
                        calendar.add(Calendar.HOUR, -8)
                        break
                    case '16hour':
                        calendar.add(Calendar.HOUR, -16)
                        break
                }
                try {
                    lesson.previousMemory.put(key, bytesFetcherService.getMemory(calendar.time))
                } catch(e){
                    e.printStackTrace()
                }
                try {
                    lesson.previousNews.put(key, bytesFetcherService.getNews(calendar.time))
                } catch(e){
                    e.printStackTrace()
                }
            }

            /** Capture associations for normalized data for instance */
            brainItUp(lesson.memory, lesson.tag.getTagName(), INSTANT_TIME_REFERENCE)

            /** Capture associations for normalized data at other time deltas */
            lesson.intervals.each { String key ->
                Memory memory = lesson.previousMemory.get(key)
                if(memory){
                    brainItUp(memory, lesson.tag.getTagName(), key)
                }
            }
            println "done"
            return
        }
        println "no lesson"
    }

    /**
     * Brain it up - generate references - and update number associations
     *
     * @param memory
     * @param tagName
     * @param timeDelta
     */
    void brainItUp(Memory memory, String tagName, String timeDelta){
        if(!memory) return
        memory.normalized.properties.each { prop, val ->
            if(val instanceof Double) {
                String tagReference = tagName + OBNOXIOUS_REFERENCE_SEPARATOR + timeDelta + OBNOXIOUS_REFERENCE_SEPARATOR + prop
                NumberAssociation numberAssociationTagInstant = bytesFetcherService.getNumberAssociation(tagReference)
                captureNewValue(numberAssociationTagInstant, Double.parseDouble(val as String))
                String defaultReference = GENERAL_ASSOCIATION_REFERENCE + OBNOXIOUS_REFERENCE_SEPARATOR + timeDelta + OBNOXIOUS_REFERENCE_SEPARATOR + prop
                NumberAssociation numberAssociationGeneralInstant = bytesFetcherService.getNumberAssociation(defaultReference)
                captureNewValue(numberAssociationGeneralInstant, Double.parseDouble(val as String))
            }
        }
    }

    /**
     * Capture new value for number association
     *
     * @param numberAssociation
     * @param value
     */
    void captureNewValue(NumberAssociation numberAssociation, double value){
        if(!numberAssociation) return
        numberAssociation.standard_deviation = NerdUtils.applyValueGetNewDeviation(
            value,
            numberAssociation.mean,
            numberAssociation.count + 1,
            numberAssociation.standard_deviation)
        numberAssociation.mean = NerdUtils.applyValueGetNewMean(
            value,
            numberAssociation.mean,
            numberAssociation.count)
        numberAssociation.count++
        bytesFetcherService.saveNumberAssociation(numberAssociation)
    }

}
