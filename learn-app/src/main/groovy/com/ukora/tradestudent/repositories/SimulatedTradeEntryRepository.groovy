package com.ukora.tradestudent.repositories

import com.ukora.tradestudent.entities.SimulatedTradeEntry
import org.springframework.data.mongodb.repository.MongoRepository

interface SimulatedTradeEntryRepository extends MongoRepository<SimulatedTradeEntry, String> {

}