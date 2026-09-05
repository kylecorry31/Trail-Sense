package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.logging.Logger
import java.time.Duration
import java.time.Instant
import kotlin.math.sqrt

class KalmanGPSModule(private val logger: Logger = getAppService()) : GPSModule {
    private var location: Coordinate? = null
    private var speed = 0f
    private var bearing: Bearing? = null
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
            updateVelocity(previousData)
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
            speed = 0f
            bearing = null
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
                // Predict from the previous fix's velocity before correcting with the new position.
                val direction = bearing
                val predictedLocation = if (direction != null && speed > 0f) {
                    lastLocation.plus(Distance.meters((speed * seconds).toFloat()), direction)
                } else {
                    lastLocation
                }
                // Uncertainty grows while GPS is stopped as well.
                val predictedVariance = variance + PROCESS_VARIANCE_PER_SECOND * seconds
                val gain = predictedVariance / (predictedVariance + measurementVariance)
                val longitudeDelta =
                    (newData.location.longitude - predictedLocation.longitude + 540.0) % 360.0 - 180.0
                location = Coordinate(
                    predictedLocation.latitude +
                        gain * (newData.location.latitude - predictedLocation.latitude),
                    (predictedLocation.longitude + gain * longitudeDelta + 540.0) % 360.0 - 180.0
                )
                variance = (1 - gain) * predictedVariance
            }
            time = newData.time
            updateVelocity(newData)
        }

        newData.location = location ?: newData.location
        newData.horizontalAccuracy = sqrt(variance).toFloat()
        return true
    }

    private fun updateVelocity(data: ModularGPSData) {
        speed = data.speed.convertTo(DistanceUnits.Meters, TimeUnits.Seconds).value
            .takeIf { it.isFinite() && it >= 0f } ?: 0f
        bearing = data.rawBearing?.takeIf { it.isFinite() }?.let { Bearing.from(it) }
            ?: data.bearing?.takeIf { it.value.isFinite() }
    }

    companion object {
        private const val TAG = "KalmanGPSModule"
        private const val DEFAULT_ACCURACY = 50.0
        private const val PROCESS_VARIANCE_PER_SECOND = 9.0
    }
}
