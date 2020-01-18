package com.kylecorry.sensorfilters

class HighPassFilter(private val alpha: Double, initialValue: Double = 0.0) : ISensorFilter {

    private var estimate = initialValue
    private var lastMeasurement = initialValue

    override fun filter(measurement: Double): Double {
        estimate = alpha * (estimate + measurement - lastMeasurement)
        lastMeasurement = measurement
        return estimate
    }
}