package com.kylecorry.sensorfilters

class LowPassFilter(private val alpha: Double, initialValue: Double = 0.0) : ISensorFilter {

    private var estimate = initialValue

    override fun filter(measurement: Double): Double {
        estimate = (1 - alpha) * estimate + alpha * measurement
        return estimate
    }
}