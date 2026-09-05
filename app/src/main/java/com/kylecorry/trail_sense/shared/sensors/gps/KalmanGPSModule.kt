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
import kotlin.math.sin
import kotlin.math.sqrt

class KalmanGPSModule(private val logger: Logger = getAppService()) : GPSModule {
    private var location: Coordinate? = null
    private var speed = 0f
    private var bearing: Bearing? = null
    private var variance = 0.0
    private var velocityVariance = 9.0
    private var reportedAccuracy = 50f
    private var time: Instant? = null

    override fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
        // Another GPS instance may have advanced the shared cache since our last update.
        val hasNewerPreviousReading = time?.let { previousData.time > it } == true
        val shouldSeedFromPrevious = location == null || hasNewerPreviousReading
        if (shouldSeedFromPrevious && previousData.location != Coordinate.zero &&
            previousData.time <= newData.time
        ) {
            restore(previousData)
        }

        if (needsReset(previousData, newData)) {
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
                Duration.between(lastTime, newData.time).let { it.seconds + it.nano / 1_000_000_000.0 }
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
                val secondsSquared = seconds * seconds
                val predictedVariance = variance + PROCESS_VARIANCE_PER_SECOND * seconds +
                    velocityVariance * secondsSquared +
                    ACCELERATION_VARIANCE * secondsSquared * secondsSquared / 4.0
                val gain = predictedVariance / (predictedVariance + measurementVariance)
                val residual = predictedLocation.distanceTo(newData.location)
                location = if (residual > 0f) {
                    newData.location.plus(
                        Distance.meters(((1 - gain) * residual).toFloat()),
                        newData.location.bearingTo(predictedLocation)
                    )
                } else {
                    predictedLocation
                }
                // Equivalent to (1 - gain) * predictedVariance, without cancellation
                // when the gain rounds to one after a long interval.
                variance = gain * measurementVariance
            }
            // Android accuracy is a 68% horizontal radius, not a calibrated posterior
            // covariance for this model.
            reportedAccuracy = maxOf(accuracy.toFloat(), sqrt(variance).toFloat())
            time = newData.time
            updateVelocity(newData)
        }

        newData.kalmanVariance = variance
        newData.kalmanVelocityVariance = velocityVariance
        newData.location = location ?: newData.location
        newData.horizontalAccuracy = reportedAccuracy
        return true
    }

    private fun restore(data: ModularGPSData) {
        location = data.location
        val accuracy = data.horizontalAccuracy
            ?.takeIf { it.isFinite() && it > 0f }?.toDouble() ?: DEFAULT_ACCURACY
        variance = data.kalmanVariance.validVariance() ?: (accuracy * accuracy)
        reportedAccuracy = accuracy.toFloat()
        time = data.time
        updateVelocity(data)
        velocityVariance = data.kalmanVelocityVariance.validVariance() ?: velocityVariance
    }

    private fun Double?.validVariance(): Double? = this?.takeIf { it.isFinite() && it >= 0.0 }

    private fun needsReset(previous: ModularGPSData, next: ModularGPSData): Boolean {
        if (next.time < previous.time) return true
        val lastTime = time ?: return false
        return next.time < lastTime
    }

    private fun updateVelocity(data: ModularGPSData) {
        speed = data.speed.convertTo(DistanceUnits.Meters, TimeUnits.Seconds).value
            .takeIf { it.isFinite() && it >= 0f } ?: 0f
        bearing = data.rawBearing?.takeIf { it.isFinite() }?.let { Bearing.from(it) }
            ?: data.bearing?.takeIf { it.value.isFinite() }
        val speedError = data.speedAccuracy?.takeIf { it.isFinite() && it > 0f }
            ?.toDouble() ?: maxOf(3.0, speed * 0.25)
        val directionError = data.bearingAccuracy?.takeIf { it.isFinite() && it >= 0f }
            ?.toDouble()?.coerceAtMost(90.0) ?: 30.0
        val lateralError = speed * sin(Math.toRadians(directionError))
        velocityVariance = if (bearing == null) {
            maxOf(9.0, speed.toDouble() * speed)
        } else {
            speedError * speedError + lateralError * lateralError
        }
    }

    companion object {
        private const val TAG = "KalmanGPSModule"
        private const val DEFAULT_ACCURACY = 50.0
        private const val PROCESS_VARIANCE_PER_SECOND = 9.0
        private const val ACCELERATION_VARIANCE = 4.0
    }
}
