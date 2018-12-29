package com.ukora.domain.repositories

import com.ukora.domain.entities.BrainNode
import org.springframework.data.mongodb.repository.MongoRepository

interface BrainNodeRepository extends MongoRepository<BrainNode, String> {

}