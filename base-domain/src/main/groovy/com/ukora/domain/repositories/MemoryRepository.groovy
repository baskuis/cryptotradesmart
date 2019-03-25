package com.ukora.domain.repositories

import com.ukora.domain.entities.Memory
import org.springframework.data.jpa.repository.Query
import org.springframework.data.mongodb.repository.MongoRepository

interface MemoryRepository extends MongoRepository<Memory, String> {

    @Query(value =
    """
        { 
            'metadata.datetime' : { 
                \$and: [
                    { \$gt: ?0 }, 
                    { \$lt: ?1 } 
                ]
            }
            'exchange.exchange': ?3,
            'exchange.details.tradecurrency': ?4,
            'exchange.details.pricecurrency': ?5
        }
    """)
    List<Memory> findByMetadataDatetimeBetween(
            Date min,
            Date max,
            String platform,
            String tradeCurrency,
            String priceCurrency
    )

}