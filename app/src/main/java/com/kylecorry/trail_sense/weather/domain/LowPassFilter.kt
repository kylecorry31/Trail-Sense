package com.kylecorry.trail_sense.weather.domain

class LowPassFilter(private val alpha: Double, initialValue: Double = 0.0) {

    private var estimate = initialValue

    fun filter(measurement: Double): Double {
        estimate = (1 - alpha) * estimate + alpha * measurement
        return estimate
    }
}