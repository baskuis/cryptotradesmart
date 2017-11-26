package com.ukora.tradestudent.utils

class NerdUtils {

    /**
     * Calculate chance of correlation against
     * the first standard deviation and mean passed
     * the highest value is 1 for perfect correlation with the first set of data
     * or -1 for perfect correlation with the second set of data
     *
     * @param double value
     * @param double theDeviation
     * @param double theMean
     * @param double commonDeviation
     * @param double commonMean
     * @return double
     */
    static chanceOfCorrelation(double value, double theDeviation, double theMean, double commonDeviation, double commonMean) {
        return (double) (2 /
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
     * @param double value
     * @param double mean
     * @param double count
     * @param double deviation
     * @return double
     */
    static applyValueGetNewDeviation(double value, double mean, double count, double deviation) {
        return (double) Math.sqrt(
            (
                (count * (Math.pow(value, 2) + (
                    (count - 1.0) * (Math.pow(deviation, 2) + Math.pow(mean, 2))
                ))) - Math.pow(value + ((count - 1.0) * mean), 2)
            ) / Math.pow(count, 2))
    }

    /**
     * Apply value get new mean
     *
     * @param double value
     * @param double mean
     * @param double count
     * @return double
     */
    static applyValueGetNewMean(double value, double mean, double count) {
        return (double) ((mean * count) + value) / (count + 1.0)
    }

}
