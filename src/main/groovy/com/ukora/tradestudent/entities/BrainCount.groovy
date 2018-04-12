package com.ukora.tradestudent.entities

import com.mongodb.DBObject

class BrainCount {

    String id
    String reference
    String source

    /**
     * Tag -> Count
     */
    Map<String, Integer> counters = [:]

    DBObject obj

}
