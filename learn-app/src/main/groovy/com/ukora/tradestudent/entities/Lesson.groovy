package com.ukora.tradestudent.entities

import com.ukora.tradestudent.tags.AbstractCorrelationTag
import org.springframework.data.annotation.Id

class Lesson extends AbstractAssociation {

    @Id
    String id

    Boolean processed
    AbstractCorrelationTag tag

}
