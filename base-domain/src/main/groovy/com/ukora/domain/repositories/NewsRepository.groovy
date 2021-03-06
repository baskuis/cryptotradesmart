package com.ukora.domain.repositories

import com.ukora.domain.entities.News
import org.springframework.data.jpa.repository.Query
import org.springframework.data.mongodb.repository.MongoRepository

interface NewsRepository extends MongoRepository<News, String> {

    @Query(value = '{ \'metadata.datetime\' : [ $and: { $gt: ?0 }, { $lt: ?1 } ] }')
    List<News> findByMetadataDatetimeBetween(Date min, Date max)

}