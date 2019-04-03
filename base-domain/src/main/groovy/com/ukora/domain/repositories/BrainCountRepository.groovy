package com.ukora.domain.repositories

import com.ukora.domain.entities.BrainCount
import org.springframework.data.mongodb.repository.MongoRepository

interface BrainCountRepository extends MongoRepository<BrainCount, String> {

    List<BrainCount> findByReference(String reference)

}