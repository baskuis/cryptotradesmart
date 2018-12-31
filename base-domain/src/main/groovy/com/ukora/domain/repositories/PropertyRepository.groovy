package com.ukora.domain.repositories

import com.ukora.domain.entities.Property
import org.springframework.data.mongodb.repository.MongoRepository

interface PropertyRepository extends MongoRepository<Property, String> {

    List<Property> findByName(String name)

}