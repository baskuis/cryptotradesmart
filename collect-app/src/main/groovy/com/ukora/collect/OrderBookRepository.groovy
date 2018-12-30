package com.ukora.collect

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.mongodb.repository.MongoRepository

interface OrderBookRepository extends MongoRepository<SourceIntegration.Book, String> {

    @Query('{ "processed" : { $ne : true }}')
    Page<SourceIntegration.Book> processedNot(Pageable pageable, boolean value)

}