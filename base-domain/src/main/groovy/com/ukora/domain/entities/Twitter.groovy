package com.ukora.domain.entities

import org.springframework.data.annotation.Id

class Twitter {

    @Id
    String id

    Statuses statuses
    Metadata metadata
}
