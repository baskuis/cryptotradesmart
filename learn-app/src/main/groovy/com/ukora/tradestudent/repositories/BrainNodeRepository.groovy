package com.ukora.tradestudent.repositories

import com.ukora.tradestudent.entities.BrainNode
import org.springframework.data.mongodb.repository.MongoRepository

interface BrainNodeRepository extends MongoRepository<BrainNode, String> {

}