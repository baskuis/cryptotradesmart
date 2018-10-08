package com.ukora.tradestudent.repositories

import com.ukora.tradestudent.entities.SimulationResult
import org.springframework.data.mongodb.repository.MongoRepository

interface SimulationResultRepository extends MongoRepository<SimulationResult, String> {

}