package com.ukora.domain.repositories

import com.ukora.domain.entities.TextCorrelationAssociation
import org.springframework.data.mongodb.repository.MongoRepository

interface TextCorrelationAssociationRepository extends MongoRepository<TextCorrelationAssociation, String> {

    List<TextCorrelationAssociation> findByDate(Date date)

}