package com.ukora.domain.repositories

import com.ukora.domain.entities.Brain
import org.springframework.data.mongodb.repository.MongoRepository

interface BrainRepository extends MongoRepository<Brain, String> {

    List<Brain> findByReferenceAndTag(String reference, String tag)

}