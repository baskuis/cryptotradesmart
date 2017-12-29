package com.ukora.tradestudent.services.learner

import spock.lang.Specification

class TraverseLessonsServiceSpec extends Specification {

    def "loop test"() {

        def c = 0
        (1..3).each {
            c++
        }

        expect:
        c == 3

    }

}
