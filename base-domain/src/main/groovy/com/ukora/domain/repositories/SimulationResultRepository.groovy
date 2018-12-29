package com.ukora.domain.repositories

import com.ukora.domain.entities.SimulationResult
import org.springframework.data.mongodb.repository.MongoRepository

interface SimulationResultRepository extends MongoRepository<SimulationResult, String> {

}