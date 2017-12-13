package com.ukora.tradestudent.entities

import com.mongodb.DBObject

abstract class AbstractAssociation {

    List<String> intervals = [
            '2minute',
            '5minute',
            '10minute',
            '15minute',
            '30minute',
            '45minute',
            '1hour',
            '2hour',
            '4hour',
            '8hour',
            '16hour'
    ]

    Exchange exchange
    String id
    Date date
    Double price
    DBObject dbObject

    Memory memory
    Twitter twitter
    List<News> news
    Map<String, Double> previousPrices = [:]
    Map<String, Memory> previousMemory = [:]
    Map<String, List<News>> previousNews = [:]

    Map<String, Map<String, Double>> associations = [:]

}
