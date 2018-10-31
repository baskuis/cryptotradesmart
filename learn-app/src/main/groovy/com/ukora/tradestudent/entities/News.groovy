package com.ukora.tradestudent.entities

import org.springframework.data.annotation.Id

class News {

    @Id
    String id

    Article article
    Metadata metadata
}
