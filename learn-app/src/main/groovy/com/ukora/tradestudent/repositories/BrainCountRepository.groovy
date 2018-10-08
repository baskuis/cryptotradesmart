package com.ukora.tradestudent.repositories

import com.ukora.tradestudent.entities.BrainCount
import org.springframework.data.mongodb.repository.MongoRepository

interface BrainCountRepository extends MongoRepository<BrainCount, String> {

    BrainCount findByReference(String reference)

}