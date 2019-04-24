package com.ukora.domain.repositories

import com.ukora.domain.entities.CorrelationAssociation
import org.springframework.data.mongodb.repository.MongoRepository


interface CorrelationAssociationRepository extends MongoRepository<CorrelationAssociation, String> {

    List<CorrelationAssociation> findByDate(Date date)

}