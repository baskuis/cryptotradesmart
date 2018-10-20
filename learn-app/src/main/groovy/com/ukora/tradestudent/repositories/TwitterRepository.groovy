package com.ukora.tradestudent.repositories

import com.ukora.tradestudent.entities.Twitter
import org.springframework.data.jpa.repository.Query
import org.springframework.data.mongodb.repository.MongoRepository

interface TwitterRepository extends MongoRepository<Twitter, String> {

    @Query(value = '{ \'metadata.datetime\' : [ $and: { $gt: ?0 }, { $lt: ?1 } ] }')
    List<Twitter> findByMetadataDatetimeBetween(Date min, Date max)

}