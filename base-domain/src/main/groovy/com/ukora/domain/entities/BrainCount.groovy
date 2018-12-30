package com.ukora.domain.entities

import org.springframework.data.annotation.Id

class BrainCount {

    @Id
    String id

    String reference
    String source

    /**
     * Tag -> Count
     */
    Map<String, Integer> counters = [:]

}
