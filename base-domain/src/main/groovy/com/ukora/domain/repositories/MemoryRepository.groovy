package com.ukora.domain.repositories

import com.ukora.domain.entities.Memory
import org.springframework.data.jpa.repository.Query
import org.springframework.data.mongodb.repository.MongoRepository

interface MemoryRepository extends MongoRepository<Memory, String> {

    /** TODO: Write test to make sure that this works */
    @Query(value =
    """
        { 
            'metadata.datetime' : { 
                \$and: [
                    { \$gt: ?0 }, 
                    { \$lt: ?1 } 
                ]
            }
            'exchange.exchange': ?2,
            'exchange.details.tradecurrency': ?3,
            'exchange.details.pricecurrency': ?4
        }
    """)
    List<Memory> customQuery(
            Date from,
            Date to,
            String platform,
            String tradeCurrency,
            String priceCurrency
    )

}