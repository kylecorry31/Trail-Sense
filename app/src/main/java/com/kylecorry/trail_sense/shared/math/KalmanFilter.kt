package com.kylecorry.trail_sense.shared.math

class KalmanFilter(private val measurementError: Double, private val processError: Double, initialEstimate: Double = Double.NaN) :
    ISensorFilter {

    private var estimateError = measurementError

    private val kalmanGain: Double
        get() = estimateError / (estimateError + measurementError)

    private var lastEstimate = initialEstimate

    override fun filter(measurement: Double): Double {
        if (lastEstimate.isNaN()){
            lastEstimate = measurement
        }

        // Predict
        estimateError += processError

        // Update
        val currentEstimate = lastEstimate + kalmanGain * (measurement - lastEstimate) // EMA

        estimateError *= (1 - kalmanGain)
        lastEstimate = currentEstimate
        return currentEstimate
    }

}