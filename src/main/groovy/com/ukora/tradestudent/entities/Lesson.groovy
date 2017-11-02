package com.ukora.tradestudent.entities

import com.mongodb.DBObject
import com.ukora.tradestudent.tags.AbstractCorrelationTag

class Lesson {

    List<String> intervals = [
            '2minute',
            '5minute',
            '10minute',
            '30minute',
            '1hour',
            '2hour',
            '4hour',
            '8hour',
            '16hour'
    ]

    Exchange exchange
    String id
    AbstractCorrelationTag tag
    Date date
    Double price
    DBObject dbObject

    Memory memory
    Twitter twitter
    List<News> news
    Map<String, Memory> previousMemory = [:]
    Map<String, List<News>> previousNews = [:]

}
