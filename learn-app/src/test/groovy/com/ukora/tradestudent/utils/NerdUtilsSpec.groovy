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
        0.3d       | 0.65d                    | -0.87d                   | true
        0d         | 1d                       | -2d                      | false
        2d         | 1d                       | -1d                      | false
        Double.NaN | 1d                       | -1d                      | false
        0d         | Double.NEGATIVE_INFINITY | -1d                      | false
        0d         | Double.NEGATIVE_INFINITY | Double.POSITIVE_INFINITY | false

    }

    @Unroll
    def "combineStandardDeviations produces #expectedValue for #value1, #value2, #value3"() {

        when:
        def v = NerdUtils.combineStandardDeviations(value1, value2, value3)

        then:
        v == expectedValue

        where:
        value1                   | value2 | value3 | expectedValue
        2                        | 3      | 4      | 5.385164807134504
        Double.NaN               | 3      | 4      | null
        Double.NEGATIVE_INFINITY | 3      | 4      | null
        Double.POSITIVE_INFINITY | 3      | 4      | null
        2                        | 2      | 2      | 3.4641016151377544

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
        null                     | false

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
        10    | 1            | 2       | 1               | 3          | -0.9988944427261528
        3     | 2            | 3.2     | 2.9             | 3.2        | 0.18240645145295198

    }

    @Unroll
    def "Test chanceOfCorrelationSoftening produces correct #expectedValue"() {

        when:
        def p = NerdUtils.chanceOfCorrelationSoftening(
                value as double,
                theDeviation as double,
                theMean as double,
                commonDeviation as double,
                commonMean as double,
                0.2)

        then:
        p == expectedValue

        where:
        value | theDeviation | theMean | commonDeviation | commonMean | expectedValue
        2.5   | 1            | 2.5     | 1               | 2.5        | 0
        100   | 1            | 2       | 100             | 2          | -0.006132398907915615
        100   | 100          | 2       | 1               | 2          | 0.006132398907915837
        2.5   | 1            | 3.5     | 1               | 2.5        | -0.15080180105374574
        2.5   | 1            | 2.5     | 1               | 3.5        | 0.15080180105374574
        2.5   | 1            | 2.5     | 1               | 3.5        | 0.15080180105374574
        0.95  | 0.3          | 1.2     | 0.4             | 0.8        | 0.004436752098924135
        100   | 1            | 2       | 1               | 3          | 0
        3     | 2            | 3.2     | 2.9             | 3.1        | 0.08287361500519319

    }

    @Unroll
    def "Applying new value gets correct new standard deviation #expectedValue"() {

        when:
        def d = NerdUtils.applyValueGetNewDeviation(
                value as double,
                mean as double,
                count as Integer,
                deviation as double)

        then:
        d == expectedValue

        where:
        value                   | mean                   | count | deviation | expectedValue
        1                       | 1                      | 1     | 1         | 0
        3                       | 1                      | 5     | 1         | 1.2
        1                       | 2                      | 2     | 1         | 0.8660254037844386
        1                       | 2                      | 3     | 1         | 0.9428090415820634
        1                       | 2                      | 4     | 1         | 0.9682458365518543
        1                       | 2                      | 5     | 1         | 0.9797958971132712
        1                       | 1                      | 2     | 1         | 0.7071067811865476
        1.1                     | 1.2                    | 3     | 0.879     | 0.719246982768939
        343.5625743637561       | 0.0                    | 1.0   | 0.0       | 0
        0.002410952215149232555 | 0.00241095221514986515 | 100   | 0.0       | 2.634178031930877E-11

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
        value                      | expected
        "2017-12-01T21:43:03.000Z" | new Date('Thu Dec 01 21:43:03 UTC 2017')

    }

    @Unroll
    def "Test emotional boundaries are correctly extracted - provided #value get #expected"() {

        when:
        Map boundaries = NerdUtils.extractBoundaryDistances(value)

        then:
        boundaries == expected

        where:
        value     | expected
        1111      | [ones: 0.0, fives: 0.2, tens: 0.1, twenties: 0.55, twentyfives: 0.44, fifties: 0.22, seventyfives: 0.8133333333333334, hundreds: 0.11, twohundreds: 0.555, fivehunderds: 0.222, thousands: 0.111]
        1275      | [ones: 0.0, fives: 0.0, tens: 0.5, twenties: 0.75, twentyfives: 0.0, fifties: 0.5, seventyfives: 0.0, hundreds: 0.75, twohundreds: 0.375, fivehunderds: 0.55, thousands: 0.275]
        1111111.1 | [ones: 0.10000000009313226, fives: 0.22000000001862646, tens: 0.11000000000931323, twenties: 0.5550000000046567, twentyfives: 0.4440000000037253, fifties: 0.22200000000186265, seventyfives: 0.8146666666679084, hundreds: 0.11100000000093133, twohundreds: 0.5555000000004656, fivehunderds: 0.22220000000018628, thousands: 0.11110000000009314, twothousands: 0.5555500000000466, fivethousands: 0.22222000000001862, tenthousands: 0.11111000000000931, twentythousands: 0.5555550000000047, fiftythousands: 0.22222200000000186, hundredthousands: 0.11111100000000093, twohundredthousands: 0.5555555000000004, fivehundredthousands: 0.22222220000000018, millions: 0.11111110000000009]

    }

    @Unroll
    def "get proportion of #value against #others produces #expected"() {

        when:
        Double v = NerdUtils.getProportionOf(value, others)

        then:
        v == expected

        where:
        value | others | expected
        1     | [1, 1] | 0.3333333333333333d
        -2    | [1, 1] | 0.5
        3     | [2]    | 0.6

    }

    @Unroll
    def "get partitionMap split map with 6 entries into #pieces pieces produces #size partitions"() {

        when:
        def r = NerdUtils.partitionMap(map, pieces)

        then:
        r.size() == size

        where:
        map                                  | pieces | size
        [:]                                  | 8      | 1
        [a: 1, b: 2, c: 3, d: 4, e: 5, f: 6] | 0      | 1
        [a: 1, b: 2, c: 3, d: 4, e: 5, f: 6] | 3      | 3
        [a: 1, b: 2, c: 3, d: 4, e: 5, f: 6] | 2      | 2
        [a: 1, b: 2, c: 3, d: 4, e: 5, f: 6] | 1      | 1
        [a: 1, b: 2, c: 3, d: 4, e: 5, f: 6] | 4      | 6
        [a: 1, b: 2, c: 3, d: 4, e: 5, f: 6] | 5      | 6
        [a: 1, b: 2, c: 3, d: 4, e: 5, f: 6] | 6      | 6
        [a: 1, b: 2, c: 3, d: 4, e: 5, f: 6] | 10     | 6

    }

    def "modifying partitioned map does not modify origin map"() {

        when:
        Map o = [a: 1, b: 2]
        def r = NerdUtils.partitionMap(o, 2)
        (r.get(1) as Map).put('a', 3)

        then:
        o.get('a') != 3

    }

}
