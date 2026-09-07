package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.trail_sense.shared.ProguardIgnore

data class GPSKalmanState(
    val state: List<Float> = emptyList(),
    val covariance: List<List<Float>> = emptyList(),
    val referenceLatitude: Double = 0.0,
    val referenceLongitude: Double = 0.0
) : ProguardIgnore
