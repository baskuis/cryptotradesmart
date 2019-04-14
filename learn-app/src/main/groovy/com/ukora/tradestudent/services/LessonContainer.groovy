package com.ukora.tradestudent.services

import com.ukora.domain.entities.Lesson
import com.ukora.tradestudent.utils.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class LessonContainer {

    Stack<Lesson> unproccessedLessons = new Stack<>()
    Stack<Lesson> textUnproccessedLessons = new Stack<>()

    @Autowired
    BytesFetcherService bytesFetcherService

    @Scheduled(initialDelay = 30000l, fixedRate = 1800000l)
    def reload(){
        unproccessedLessons.removeAllElements()
        unproccessedLessons.addAll(bytesFetcherService.unproccessedLessons())
        textUnproccessedLessons.removeAllElements()
        textUnproccessedLessons.addAll(bytesFetcherService.unproccessedTextLessons())
    }

    /**
     * Get the next un-processed lesson
     *
     * @return
     */
    Lesson getNextTextLesson() {
        try {
            return textUnproccessedLessons.pop()
        } catch (EmptyStackException e){
            Logger.debug('textUnproccessedLessons empty')
        }
        return null
    }

    /**
     * Get the next un-processed lesson
     *
     * @return
     */
    Lesson getNextLesson() {
        try {
            return unproccessedLessons.pop()
        } catch (EmptyStackException e){
            Logger.debug('unproccessedLessons empty')
        }
        return null
    }

}
