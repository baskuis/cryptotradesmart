package com.ukora.domain.entities

import org.springframework.data.annotation.Id

class Lesson extends AbstractAssociation {

    @Id
    String id

    Boolean processed
    Boolean textProcessed
    String tag

}
