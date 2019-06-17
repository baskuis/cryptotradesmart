package com.ukora.domain.repositories

import com.ukora.domain.entities.TextCorrelationAssociation
import org.springframework.data.jpa.repository.Query
import org.springframework.data.mongodb.repository.MongoRepository

interface TextCorrelationAssociationRepository extends MongoRepository<TextCorrelationAssociation, String> {

    List<TextCorrelationAssociation> findByDate(Date date)

    @Query(value = '{ \'date\' : [ $and: { $gt: ?0 }, { $lt: ?1 } ] }')
    List<TextCorrelationAssociation> findByDateBetween(Date min, Date max)

}