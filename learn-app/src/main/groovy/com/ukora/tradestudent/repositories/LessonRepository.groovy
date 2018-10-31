package com.ukora.tradestudent.repositories

import com.ukora.tradestudent.entities.Lesson
import org.springframework.data.mongodb.repository.MongoRepository

interface LessonRepository extends MongoRepository<Lesson, String> {

    Integer getCountByTag(String tag)

    Lesson findByTextProcessedNot(Boolean textProcessed)

    Lesson findByProcessedNot(Boolean processed)

}