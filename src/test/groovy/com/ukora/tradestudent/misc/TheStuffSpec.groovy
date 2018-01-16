package com.ukora.tradestudent.misc

import spock.lang.Specification

import java.text.SimpleDateFormat

class TheStuffSpec extends Specification {

    def "Test date format conversion"() {

        given:
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.mmm'Z'")
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"))

        when:
        Date date = sdf.parse(value)

        then:
        date.time == expected

        where:
        value | expected
        "2017-12-01T21:43:03.122Z" | 1512169323000l

    }

    def "test incrementing value in map"() {

        given:
        Map<String, Integer> incMap = [:]

        when:
        incMap.put('v', incMap.get('v', 0) + 1)
        incMap.put('v', incMap.get('v', 0) + 1)

        then:
        incMap.get('v') == 2

    }

}
