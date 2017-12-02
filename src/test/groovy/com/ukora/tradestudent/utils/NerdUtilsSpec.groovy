package com.ukora.tradestudent.utils

import spock.lang.Specification
import spock.lang.Unroll

import java.text.SimpleDateFormat

class NerdUtilsSpec extends Specification {

    @Unroll
    def "Assert values are withing range #value1, #value2, #value3 produces #expectedValue"() {

        when:
        def v = NerdUtils.assertRanges(value1 as Double, value2 as Double, value3 as Double)

        then:
        v == expectedValue

        where:
        value1     | value2                   | value3                   | expectedValue
        0d         | 1d                       | -1d                      | true
        2d         | 1d                       | -1d                      | false
        Double.NaN | 1d                       | -1d                      | false
        0d         | Double.NEGATIVE_INFINITY | -1d                      | false
        0d         | Double.NEGATIVE_INFINITY | Double.POSITIVE_INFINITY | false

    }

    @Unroll
    def "Assert value is within range #value produces #expectedValue"() {

        when:
        def v = NerdUtils.assertRange(value as Double)

        then:
        v == expectedValue

        where:
        value                    | expectedValue
        0d                       | true
        2d                       | false
        1d                       | true
        -1d                      | true
        -1.1d                    | false
        Double.NaN               | false
        Double.NEGATIVE_INFINITY | false
        Double.POSITIVE_INFINITY | false

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
        2.5   | 1            | 2.5     | 1               | 2.5        | 0
        100   | 1            | 2       | 100             | 2          | -1
        100   | 100          | 2       | 1               | 2          | 1
        2.5   | 1            | 3.5     | 1               | 2.5        | -0.24491866240370908
        2.5   | 1            | 2.5     | 1               | 3.5        | 0.2449186624037092

    }

    @Unroll
    def "Applying new value gets correct new standard deviation #expectedValue"() {

        when:
        def d = NerdUtils.applyValueGetNewDeviation(
                value as double,
                mean as double,
                count as double,
                deviation as double)

        then:
        d == expectedValue

        where:
        value             | mean | count | deviation | expectedValue
        1                 | 1    | 1     | 1         | 0
        3                 | 1    | 5     | 1         | 1.2
        1                 | 2    | 2     | 1         | 0.8660254037844386
        1                 | 2    | 3     | 1         | 0.9428090415820634
        1                 | 2    | 4     | 1         | 0.9682458365518543
        1                 | 2    | 5     | 1         | 0.9797958971132712
        1                 | 1    | 2     | 1         | 0.7071067811865476
        1.1               | 1.2  | 3     | 0.879     | 0.719246982768939
        343.5625743637561 | 0.0  | 1.0   | 0.0       | 0

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
        value        | mean | count | expectedValue
        1            | 1    | 1     | 1
        2            | 1    | 1     | 1.5
        3            | 1    | 1     | 2
        3            | 1    | 10    | 1.1818181818181819
        2            | 0    | 0     | 2
        343.24561234 | 0    | 0     | 343.24561234

    }

    @Unroll
    def "Test date conversion using simpleDateFormat for accuracy"() {

        setup:
        SimpleDateFormat sdf = NerdUtils.getGMTDateFormat()

        when:
        Date date = sdf.parse(value)

        then:
        date == expected

        where:
        value | expected
        "2017-12-01T21:43:03.000Z" | new Date('Thu Dec 01 21:43:03 UTC 2017')

    }

}
