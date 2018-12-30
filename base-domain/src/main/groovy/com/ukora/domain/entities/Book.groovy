package com.ukora.domain.entities

import org.bson.Document
import org.springframework.data.annotation.Id


class Book {

    @Id
    String id

    Document orderBook
    String integrationType
    Date timestamp
    String baseCode
    String counterCode
    boolean processed = false
}