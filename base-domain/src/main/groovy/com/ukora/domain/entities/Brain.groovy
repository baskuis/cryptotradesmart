package com.ukora.domain.entities


import org.springframework.data.annotation.Id

class Brain {

    @Id
    String id

    String reference
    String tag
    Double mean
    Double standard_deviation
    Integer count

}
