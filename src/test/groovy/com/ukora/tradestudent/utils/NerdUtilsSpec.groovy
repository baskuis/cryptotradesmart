package com.ukora.tradestudent.utils

import spock.lang.Specification
import spock.lang.Unroll

class NerdUtilsSpec extends Specification {

    def "10 times test"() {
        def s = ""
        10.times {
            s += "."
        }
        expect:
        s.length() == 10
    }

    @Unroll
    def "Test chanceOfCorrelation produces correct value #expectedValue"() {

        when:
        def p = NerdUtils.chanceOfCorrelation(
                value as double,
                theDeviation as double,
                theMean as double,
                commonDeviation as double,
                commonMean as double)

        then:
        p == expectedValue

        where:
        value | theDeviation | theMean | commonDeviation | commonMean | expectedValue
        2.5 | 1 | 2.5 | 1 | 2.5 | 0
        100 | 1 | 2 | 100 | 2 | -1
        100 | 100 | 2 | 1 | 2 | 1
        2.5 | 1 | 3.5 | 1 | 2.5 | -0.24491866240370908
        2.5 | 1 | 2.5 | 1 | 3.5 | 0.2449186624037092

    }

    @Unroll
    def "Applying new value get new deviation #expectedValue"() {

        when:
        def d = NerdUtils.applyValueGetNewDeviation(
                value as double,
                mean as double,
                count as double,
                deviation as double)

        then:
        d == expectedValue

        where:
        value | mean | count | deviation | expectedValue
        1 | 1 | 1 | 1 | 0
        3 | 1 | 5 | 1 | 1.2
        1 | 2 | 2 | 1 | 0.8660254037844386
        1 | 2 | 3 | 1 | 0.9428090415820634
        1 | 2 | 4 | 1 | 0.9682458365518543
        1 | 2 | 5 | 1 | 0.9797958971132712
        1 | 1 | 2 | 1 | 0.7071067811865476
        1.1 | 1.2 | 3 | 0.879 | 0.719246982768939
        343.5625743637561 | 0.0 | 1.0 | 0.0 | 0

    }

    @Unroll
    def "Applying new value gets new mean #expectedValue"() {

        when:
        def m = NerdUtils.applyValueGetNewMean(
                value as double,
                mean as double,
                count as double)

        then:
        m == expectedValue

        where:
        value | mean | count | expectedValue
        1 | 1 | 1 | 1
        2 | 1 | 1 | 1.5
        3 | 1 | 1 | 2
        3 | 1 | 10 | 1.1818181818181819
        2 | 0 | 0 | 2
        343.24561234 | 0 | 0 | 343.24561234

    }

}
