package com.ukora.tradestudent.entities

import com.mongodb.DBObject

class Brain {
    String id
    String reference
    DBObject obj
    String tag
    Double mean
    Double standard_deviation
    Integer count
}
