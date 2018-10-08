package com.ukora.tradestudent.repositories

import com.ukora.tradestudent.entities.Brain
import org.springframework.data.mongodb.repository.MongoRepository

interface BrainRepository extends MongoRepository<Brain, String> {

    List<Brain> findByReferenceAndTag(String reference, String tag)

}