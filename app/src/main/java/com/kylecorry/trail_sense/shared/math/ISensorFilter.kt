package com.kylecorry.trail_sense.shared.math

interface ISensorFilter {

    /**
     * Filters a measurement
     * @param measurement the raw measurement to filter
     */
    fun filter(measurement: Double): Double

}