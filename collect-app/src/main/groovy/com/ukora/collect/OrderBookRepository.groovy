package com.ukora.collect

import com.ukora.domain.entities.Book
import org.springframework.data.mongodb.repository.MongoRepository

interface OrderBookRepository extends MongoRepository<Book, String> {

    List<Book> findByProcessed(boolean processed)

}