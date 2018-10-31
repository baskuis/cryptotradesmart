package com.ukora.tradestudent.entities

import org.springframework.data.annotation.Id

class Twitter {

    @Id
    String id

    Statuses statuses
    Metadata metadata
}
