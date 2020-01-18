package com.kylecorry.sensorfilters

import kotlin.math.abs

class MultiKalmanFilter(private val measurementError: Collection<Double>, private val processError: List<Double>, initialEstimate: Double = Double.NaN) : ISensorCombinationFilter {

    var estimateError: MutableList<Double> = measurementError.toMutableList()

    val kalmanGain: List<Double>
        get() {
            val l = mutableListOf<Double>()
            for (i in estimateError.indices){
                l.add(estimateError[i] / (estimateError.sum() + measurementError.sum()))
            }
            return l
        }

    private var lastEstimate = initialEstimate

    override fun filter(measurements: List<Double>): Double {

        if (lastEstimate.isNaN()){
            lastEstimate = measurements.average()
        }

        // Predict
        var currentEstimate = lastEstimate

        for (i in estimateError.indices){
            estimateError[i] = estimateError[i] + processError[i]
        }

        // Update
        val k = kalmanGain

        for (i in k.indices){
            currentEstimate += k[i] * (measurements[i] - lastEstimate)
        }

        for (i in k.indices){
            estimateError[i] = (1 - k[i]) * estimateError[i]
        }

        lastEstimate = currentEstimate
        return currentEstimate
    }

}