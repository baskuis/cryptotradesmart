package com.ukora.domain.entities

import org.springframework.data.annotation.Transient

abstract class AbstractAssociation {

    Date date

    Double price

    @Transient
    List<String> intervals = [
            '1minute',
            '2minute',
            '3minute',
            '4minute',
            '5minute',
            '6minute',
            '7minute',
            '8minute',
            '9minute',
            '10minute',
            '15minute',
            '20minute',
            '30minute',
            '45minute',
            '1hour',
            '2hour',
            '4hour',
            '8hour',
            '16hour',
            '24hour'
    ]

    @Transient
    Exchange exchange

    @Transient
    Memory memory
    @Transient
    Twitter twitter
    @Transient
    List<News> news
    @Transient
    Map<String, Double> previousPrices = [:]
    @Transient
    Map<String, Memory> previousMemory = [:]
    @Transient
    Map<String, List<News>> previousNews = [:]
    @Transient
    Map<String, Map<String, Double>> associations = [:]

}
