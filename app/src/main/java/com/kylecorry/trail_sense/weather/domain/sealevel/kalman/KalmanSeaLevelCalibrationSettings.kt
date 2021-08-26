package com.kylecorry.trail_sense.weather.domain.sealevel.kalman

data class KalmanSeaLevelCalibrationSettings(
    val altitudeOutlierThreshold: Float,
    val altitudeSmoothing: Float,
    val pressureSmoothing: Float,
    val useTemperature: Boolean,
    val useAltitudeVariance: Boolean = true
)
