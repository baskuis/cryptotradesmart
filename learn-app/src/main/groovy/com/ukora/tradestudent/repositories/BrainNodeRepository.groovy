package com.ukora.tradestudent.repositories

import com.ukora.tradestudent.entities.Lesson
import org.springframework.data.mongodb.repository.MongoRepository

interface BrainNodeRepository extends MongoRepository<Lesson, String> {

}