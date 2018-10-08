package com.ukora.tradestudent.entities

import org.springframework.data.annotation.Id

class Property {

    @Id
    String id

    String name
    String value
}
