package com.kylecorry.trail_sense.shared.sensors.gps

import java.time.Duration

data class GPSAccuracyFilter(
    val minAccuracy: Float?,
    val maxAccuracyWait: Duration?
) {
    companion object {
        const val MIN_ACCURACY_METERS = 1
        const val MAX_ACCURACY_METERS = 100
        const val MIN_WAIT_SECONDS = 1
        const val MAX_WAIT_SECONDS = 15

        val None = GPSAccuracyFilter(null, null)
        val Default = GPSAccuracyFilter(32f, Duration.ofSeconds(3))

        fun custom(minAccuracyMeters: Float, maxWaitSeconds: Int): GPSAccuracyFilter {
            return GPSAccuracyFilter(
                minAccuracyMeters.coerceIn(
                    MIN_ACCURACY_METERS.toFloat(),
                    MAX_ACCURACY_METERS.toFloat()
                ),
                Duration.ofSeconds(
                    maxWaitSeconds.coerceIn(MIN_WAIT_SECONDS, MAX_WAIT_SECONDS).toLong()
                )
            )
        }
    }
}
