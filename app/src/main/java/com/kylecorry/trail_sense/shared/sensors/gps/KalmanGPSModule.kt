package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.logging.Logger
import java.time.Duration
import java.time.Instant
import kotlin.math.sqrt

class KalmanGPSModule(private val logger: Logger = getAppService()) : GPSModule {
    private var location: Coordinate? = null
    private var variance = 0.0
    private var time: Instant? = null

    override fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
        // Another GPS instance may have advanced the shared cache since our last update.
        val hasNewerPreviousReading = time?.let { previousData.time > it } == true
        val shouldSeedFromPrevious = location == null || hasNewerPreviousReading
        if (shouldSeedFromPrevious && previousData.location != Coordinate.zero &&
            previousData.time <= newData.time
        ) {
            location = previousData.location
            val accuracy = previousData.horizontalAccuracy
                ?.takeIf { it.isFinite() && it > 0f }?.toDouble() ?: DEFAULT_ACCURACY
            variance = accuracy * accuracy
            time = previousData.time
        }

        if (newData.time < previousData.time || time?.let { newData.time < it } == true) {
            logger.debug(
                TAG,
                "Kalman filter reset: fix time moved backward " +
                    "(new: ${newData.time}, previous: ${previousData.time}, filter: $time)"
            )
            location = null
            variance = 0.0
            time = null
        }

        val lastTime = time
        val lastLocation = location
        // The location cache stores timestamps with millisecond precision.
        val sameFix = lastTime != null &&
            lastTime.toEpochMilli() == newData.time.toEpochMilli()

        if (!sameFix) {
            val seconds = if (lastTime != null) {
                Duration.between(lastTime, newData.time).toMillis() / 1000.0
            } else {
                0.0
            }

            val accuracy = newData.horizontalAccuracy
                ?.takeIf { it.isFinite() && it > 0f }?.toDouble() ?: DEFAULT_ACCURACY
            val measurementVariance = accuracy * accuracy
            if (lastLocation == null) {
                location = newData.location
                variance = measurementVariance
            } else {
                // A random-walk model: uncertainty grows while GPS is stopped as well.
                val predictedVariance = variance + PROCESS_VARIANCE_PER_SECOND * seconds
                val gain = predictedVariance / (predictedVariance + measurementVariance)
                val longitudeDelta =
                    (newData.location.longitude - lastLocation.longitude + 540.0) % 360.0 - 180.0
                location = Coordinate(
                    lastLocation.latitude + gain * (newData.location.latitude - lastLocation.latitude),
                    (lastLocation.longitude + gain * longitudeDelta + 540.0) % 360.0 - 180.0
                )
                variance = (1 - gain) * predictedVariance
            }
            time = newData.time
        }

        newData.location = location ?: newData.location
        newData.horizontalAccuracy = sqrt(variance).toFloat()
        return true
    }

    companion object {
        private const val TAG = "KalmanGPSModule"
        private const val DEFAULT_ACCURACY = 50.0
        private const val PROCESS_VARIANCE_PER_SECOND = 9.0
    }
}
