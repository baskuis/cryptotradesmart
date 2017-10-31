package com.ukora.tradestudent.services

import com.ukora.tradestudent.entities.Lesson
import groovy.util.logging.Log4j2
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct

@Log4j2
@Service
class CaptureAssociationsService {

    @Autowired
    BytesFetcherService bytesFetcherService

    @PostConstruct
    init(){
        bytesFetcherService.resetLessons()
    }

    @Scheduled(cron = "* * * * * *")
    learn(){
        Lesson lesson = bytesFetcherService.getNextLesson()
        if(lesson) {
            try {
                lesson.memory = bytesFetcherService.getMemory(lesson.date)
                if(lesson.memory == null) return
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
        }
        println "next"
    }

}
