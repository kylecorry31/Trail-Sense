package com.kylecorry.sensorfilters

interface ISensorCombinationFilter {

    /**
     * Filters a measurement
     * @param measurements the raw measurements to filter
     */
    fun filter(measurements: List<Double>): Double

}