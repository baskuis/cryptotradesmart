package com.ukora.tradestudent.utils

import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentHashMap

class NerdUtils {

    /**
     * Assert multiple p ranges at one
     *
     * @param values
     * @return
     */
    static boolean assertRanges(Double... values) {
        return !values.collect { assertRange(it) }.contains(false)
    }

    /**
     * Assert value is within expected range
     * Added 0000000000000002 decimal to account for
     * double decimal inaccuracies
     *
     * @param value
     * @return
     */
    static boolean assertRange(Double value) {
        !(value == null || value.naN || value.infinite || value < -1.0000000000000002 || value > 1.0000000000000002)
    }

    /**
     * List of emotional/psychological boundaries
     *
     *
     */
    public static Map<String, Integer> resistanceBoundaries = [
            'ones'                : 1,
            'fives'               : 5,
            'tens'                : 10,
            'twenties'            : 20,
            'twentyfives'         : 25,
            'fifties'             : 50,
            'seventyfives'        : 75,
            'hundreds'            : 100,
            'twohundreds'         : 200,
            'fivehunderds'        : 500,
            'thousands'           : 1000,
            'twothousands'        : 2000,
            'fivethousands'       : 5000,
            'tenthousands'        : 10000,
            'twentythousands'     : 20000,
            'fiftythousands'      : 50000,
            'hundredthousands'    : 100000,
            'twohundredthousands' : 200000,
            'fivehundredthousands': 500000,
            'millions'            : 1000000
    ]

    /**
     * Get emotional boundaries map of provided value
     *
     * @param value
     * @return Map
     */
    static Map<String, Double> extractBoundaryDistances(Double value) {
        Map response = [:]
        resistanceBoundaries.each {
            if (it.value <= value) {
                response[it.key] = (value % it.value) / it.value
            }
        }
        return response
    }

    /**
     * Combine multiple standard deviation values
     *
     * @param standardDeviation
     * @return Double
     */
    static Double combineStandardDeviations(Double... standardDeviation) {
        if (!standardDeviation || standardDeviation.size() < 2 || standardDeviation.findAll({
            it.naN || it.infinite
        })?.size() > 0) {
            return null
        }
        Double sumProduct = 0
        standardDeviation.each { sumProduct += Math.pow(it, 2) }
        return Math.sqrt(sumProduct)
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
     * Chance of correlation with softening factor
     *
     * @param value
     * @param theDeviation
     * @param theMean
     * @param commonDeviation
     * @param commonMean
     * @param softeningFactor
     * @return
     */
    static Double chanceOfCorrelationSoftening(Double value, Double theDeviation, Double theMean, Double commonDeviation, Double commonMean, Double softeningFactor) {
        return (Double) 2 * (((1 /
                Math.pow(2 * Math.PI * Math.pow(theDeviation, 2), 0.5) *
                Math.pow(Math.E, (-Math.pow(value - theMean, 2) / (2 * Math.pow(theDeviation, 2))))
        ) + softeningFactor) / ((1 /
                Math.pow(2 * Math.PI * Math.pow(commonDeviation, 2), 0.5) *
                Math.pow(Math.E, (-Math.pow(value - commonMean, 2) / (2 * Math.pow(commonDeviation, 2))))
        ) + (1 /
                Math.pow(2 * Math.PI * Math.pow(theDeviation, 2), 0.5) *
                Math.pow(Math.E, (-Math.pow(value - theMean, 2) / (2 * Math.pow(theDeviation, 2))))
        ) + (2 * softeningFactor))) - 1
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
    @Deprecated
    static Double applyValueGetNewDeviation(Double value, Double mean, Double count, Double deviation) {
        return (Double) Math.sqrt(
                (
                        (count * (Math.pow(value, 2.0) + (
                                (count - 1.0) * (Math.pow(deviation, 2.0) + Math.pow(mean, 2.0))
                        ))) - Math.pow(value + ((count - 1.0) * mean), 2.0)
                ) / Math.pow(count, 2.0))
    }

    /**
     * Apply value to data set
     * and get new standard deviation
     *
     * @param value
     * @param mean
     * @param count
     * @param deviation
     * @return
     */
    static Double applyValueGetNewDeviationAlt(Double value, Double mean, Double count, Double deviation) {
        Double newMean = applyValueGetNewMean(value, mean, count)
        return (Double) Math.sqrt(
                (
                        ((count) * Math.pow(deviation, 2.0)) +
                                ((value - newMean) * (value - mean))
                ) / (count + 1)
        )
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
    static SimpleDateFormat getGMTDateFormat() {
        SimpleDateFormat sdf = new SimpleDateFormat(JS_FRIENDLY_DATE_FORMAT)
        sdf.setLenient(true)
        sdf.setTimeZone(TimeZone.getTimeZone(NORMALIZED_TIMEZONE))
        sdf
    }

    /**
     * Get proportion of value among value and others
     *
     * @param value
     * @param others
     * @return
     */
    static Double getProportionOf(Double value, List<Double> others) {
        Double total = Math.abs(value)
        others.each { total += Math.abs(it) }
        return Math.abs(value / total)
    }

    /**
     * Split list into multiple pieces
     *
     * @param delegate
     * @param pieces
     * @return
     */
    static List partitionMap(Map delegate, int pieces) {
        if(!delegate || !pieces || delegate.size() == 0) return [delegate]
        if(pieces < 1) return [delegate]
        int size = Math.abs(delegate.size() / pieces)
        List r = delegate.inject([new ConcurrentHashMap()]) { List ret, elem -> (ret.last() << elem).size() >= size ? ret << new ConcurrentHashMap() : ret }
        r.last() ? r : r[0..-2]
    }

    static List<SimpleDateFormat> knownPatterns = new ArrayList<SimpleDateFormat>()
    static {
        knownPatterns.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH))
        knownPatterns.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm.ss'Z'", Locale.ENGLISH))
        knownPatterns.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH))
        knownPatterns.add(new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss", Locale.ENGLISH))
        knownPatterns.add(new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH))
    }

    /**
     * Parse date string
     *
     * @param dateString
     * @return
     */
    static Date parseString(String dateString){
        for (SimpleDateFormat pattern : knownPatterns) {
            try {
                return new Date(pattern.parse(dateString).getTime())
            } catch (Exception e) { /** Ignore */ }
        }
        return null
    }

}
