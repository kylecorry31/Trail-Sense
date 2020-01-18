package com.kylecorry.sensorfilters

interface ISensorFilter {

    /**
     * Filters a measurement
     * @param measurement the raw measurement to filter
     */
    fun filter(measurement: Double): Double

}