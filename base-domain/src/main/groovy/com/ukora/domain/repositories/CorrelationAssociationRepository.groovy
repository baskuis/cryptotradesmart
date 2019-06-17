package com.ukora.domain.repositories

import com.ukora.domain.entities.CorrelationAssociation
import org.springframework.data.jpa.repository.Query
import org.springframework.data.mongodb.repository.MongoRepository

interface CorrelationAssociationRepository extends MongoRepository<CorrelationAssociation, String> {

    List<CorrelationAssociation> findByDate(Date date)

    @Query(value = '{ \'date\' : [ $and: { $gt: ?0 }, { $lt: ?1 } ] }')
    List<CorrelationAssociation> findByDateBetween(Date min, Date max)

}