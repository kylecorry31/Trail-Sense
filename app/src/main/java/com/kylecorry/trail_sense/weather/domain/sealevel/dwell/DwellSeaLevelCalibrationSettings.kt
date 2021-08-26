package com.kylecorry.trail_sense.weather.domain.sealevel.dwell

import java.time.Duration

data class DwellSeaLevelCalibrationSettings(
    val dwellThreshold: Duration,
    val altitudeChangeThreshold: Float,
    val pressureChangeThreshold: Float?,
    val useTemperature: Boolean,
    val interpolateAltitudeChanges: Boolean
)
