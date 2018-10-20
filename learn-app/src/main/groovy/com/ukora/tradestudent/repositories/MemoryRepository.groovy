package com.ukora.tradestudent.repositories

import com.ukora.tradestudent.entities.Memory
import org.springframework.data.jpa.repository.Query
import org.springframework.data.mongodb.repository.MongoRepository

interface MemoryRepository extends MongoRepository<Memory, String> {

    @Query(value = '{ \'metadata.datetime\' : [ $and: { $gt: ?0 }, { $lt: ?1 } ] }')
    List<Memory> findByMetadataDatetimeBetween(Date min, Date max)

}