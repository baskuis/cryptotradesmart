package com.ukora.tradestudent.repositories

import com.ukora.tradestudent.entities.Property
import org.springframework.data.mongodb.repository.MongoRepository

interface PropertyRepository extends MongoRepository<Property, String> {

    Property findByName(String name);

}