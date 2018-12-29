package com.ukora.domain.repositories

import com.ukora.domain.entities.Lesson
import org.springframework.data.mongodb.repository.MongoRepository

interface LessonRepository extends MongoRepository<Lesson, String> {

    Long countByTag(String tag)

    Lesson findByTextProcessedNot(Boolean textProcessed)

    Lesson findByProcessedNot(Boolean processed)

}