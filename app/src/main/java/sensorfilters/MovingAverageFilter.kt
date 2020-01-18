package com.kylecorry.sensorfilters

import java.lang.RuntimeException

class MovingAverageFilter(size: Int, initialValue: Double = 0.0) : ISensorFilter {
    private val values = mutableListOf<Double>()
    private var currIdx = 0

    init {
        if (size <= 0){
            throw RuntimeException("Size must be greater than 0.")
        }
        for (i in 0..size){
            values.add(initialValue)
        }
    }

    override fun filter(measurement: Double): Double {
        values[currIdx] = measurement
        currIdx = (currIdx + 1) % values.size
        return values.average()
    }
}