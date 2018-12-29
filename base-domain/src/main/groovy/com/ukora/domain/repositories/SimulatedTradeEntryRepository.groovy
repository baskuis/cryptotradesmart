package com.ukora.domain.repositories

import com.ukora.domain.entities.SimulatedTradeEntry
import org.springframework.data.mongodb.repository.MongoRepository

interface SimulatedTradeEntryRepository extends MongoRepository<SimulatedTradeEntry, String> {

}