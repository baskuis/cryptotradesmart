package com.ukora.domain.entities

import com.ukora.domain.beans.tags.AbstractCorrelationTag
import org.springframework.data.annotation.Id

class Lesson extends AbstractAssociation {

    @Id
    String id

    Boolean processed
    Boolean textProcessed
    AbstractCorrelationTag tag

}
