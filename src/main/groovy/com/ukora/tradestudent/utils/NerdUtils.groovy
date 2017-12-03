package com.ukora.tradestudent.utils

import java.text.SimpleDateFormat

class NerdUtils {

    /**
     * Assert multiple p ranges at one
     *
     * @param values
     * @return
     */
    static boolean assertRanges(Double... values){
        return !values.collect{ assertRange(it) }.contains(false)
    }

    /**
     * Assert value is within expected range
     *
     * @param value
     * @return
     */
    static boolean assertRange(Double value){
        !(value.naN || value.infinite || value < -1 || value > 1)
    }

    /**
     * List of emotional boundaries
     *
     *
     */
    public static Map<String, Integer> resistanceBoundaries = [
            'ones' : 1,
            'fives' : 5,
            'tens' : 10,
            'twenties' : 20,
            'twentyfives' : 25,
            'fifties' : 50,
            'seventyfives' : 75,
            'hundreds' : 100,
            'twohundreds' : 200,
            'fivehunderds' : 500,
            'thousands' : 1000,
            'twothousands' : 2000,
            'fivethousands' : 5000,
            'tenthousands' : 10000,
            'twentythousands' : 20000,
            'fiftythousands' : 50000,
            'hundredthousands' : 100000,
            'twohundredthousands' : 200000,
            'fivehundredthousands' : 5000000,
            'millions' : 1000000
    ]

    /**
     * Get emotional boundaries map of provided value
     *
     * @param value
     * @return
     */
    static Map<String, Double> extractBoundryDistances(Double value){
        Map response = [:]
        resistanceBoundaries.each { if(it.value <= value){ response[it.key] = (value % it.value) / it.value } }
        return response
    }

    /**
     * Calculate chance of correlation against
     * the first standard deviation and mean passed
     * the highest value is 1 for perfect correlation with the first set of data
     * or -1 for perfect correlation with the second set of data
     *
     * @param Double value
     * @param Double theDeviation
     * @param Double theMean
     * @param Double commonDeviation
     * @param Double commonMean
     * @return Double
     */
    static Double chanceOfCorrelation(Double value, Double theDeviation, Double theMean, Double commonDeviation, Double commonMean) {
        return (Double) (2 /
            Math.pow(2 * Math.PI * Math.pow(theDeviation, 2), 0.5) *
            Math.pow(Math.E, (-Math.pow(value - theMean, 2) / (2 * Math.pow(theDeviation, 2)))) /
            (
                (1 /
                    Math.pow(2 * Math.PI * Math.pow(commonDeviation, 2), 0.5) *
                    Math.pow(Math.E, (-Math.pow(value - commonMean, 2) / (2 * Math.pow(commonDeviation, 2))))
                ) + (1 /
                    Math.pow(2 * Math.PI * Math.pow(theDeviation, 2), 0.5) *
                    Math.pow(Math.E, (-Math.pow(value - theMean, 2) / (2 * Math.pow(theDeviation, 2))))
                )
            )
        ) - 1
    }

    /**
     * Apply value to data set
     * and get new standard deviation
     *
     * @param Double value
     * @param Double mean
     * @param Double count
     * @param Double deviation
     * @return Double
     */
    static Double applyValueGetNewDeviation(Double value, Double mean, Double count, Double deviation) {
        return (Double) Math.sqrt(
            (
                (count * (Math.pow(value, 2) + (
                    (count - 1.0) * (Math.pow(deviation, 2) + Math.pow(mean, 2))
                ))) - Math.pow(value + ((count - 1.0) * mean), 2)
            ) / Math.pow(count, 2))
    }

    /**
     * Apply value get new mean
     *
     * @param Double value
     * @param Double mean
     * @param Double count
     * @return Double
     */
    static Double applyValueGetNewMean(Double value, Double mean, Double count) {
        return (Double) ((mean * count) + value) / (count + 1.0)
    }

    static final String JS_FRIENDLY_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
    static final String NORMALIZED_TIMEZONE = "GMT"

    /**
     * Return correct date format interpreter
     *
     * @return
     */
    static SimpleDateFormat getGMTDateFormat(){
        SimpleDateFormat sdf = new SimpleDateFormat(JS_FRIENDLY_DATE_FORMAT)
        sdf.setLenient(true)
        sdf.setTimeZone(TimeZone.getTimeZone(NORMALIZED_TIMEZONE))
        sdf
    }

}
