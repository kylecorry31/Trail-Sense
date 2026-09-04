package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.sol.math.MathExtensions.real
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.safeRoundPlaces
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.hypot

class BadReadingRejectionGPS(baseGPS: ISatelliteGPS) : BaseGPS(baseGPS) {

    private val userPrefs = getAppService<UserPreferences>()
    private val logger = getAppService<Logger>()
    private val diagnosticId = nextDiagnosticId.getAndIncrement()

    override fun onLocationUpdate() {
        if (shouldAcceptNewReading()) {
            super.onLocationUpdate()
        }
    }

    /**
     * This should only reject clearly erroneous readings.
     */
    private fun shouldAcceptNewReading(): Boolean {
        if (!baseGPS.hasValidReading) {
            return false
        }

        if (!userPrefs.filterLocationReadings) {
            return true
        }

        // The new reading is so inaccurate that it can't be useful
        val newAccuracy = baseGPS.horizontalAccuracy?.takeIf { it > 0f } ?: DEFAULT_ACCURACY
        if (newAccuracy > MAX_ACCEPTABLE_ACCURACY) {
            logRejectedReading("poor accuracy")
            return false
        }

        // If satellite count is null, then the phone doesn't support satellite count
        val satelliteCount = baseGPS.satellites
        val hasFix = satelliteCount == null || !userPrefs.requiresSatellites || satelliteCount >= 4
        if (!hasFix) {
            logRejectedReading("not enough satellites ($satelliteCount)")
            return false
        }

        if (_location == Coordinate.zero) {
            logAcceptedReading("no previous reading")
            return true
        }

        // The current reading is somehow in the future, so just accept a new reading (prevents stuck readings)
        val isLastTimeInFuture = _time.isAfter(Instant.now().plusMillis(500))
        if (isLastTimeInFuture) {
            logAcceptedReading("last reading is in the future")
            return true
        }

        // The new reading is older than the current one, so reject it
        val timeDelta = Duration.between(_time, baseGPS.time)
        if (timeDelta < Duration.ZERO) {
            logRejectedReading("older")
            return false
        }

        // The new reading is farther away than the user could have possibly traveled since the last one
        val seconds = timeDelta.toMillis() / 1000f
        val distance = _location.distanceTo(baseGPS.location)
        val speedLimit = _speed.value.real(0f)
            .times(1.5f)
            .plus(0.5f)
            .coerceIn(MIN_SPEED_ALLOWANCE, MAX_SPEED_ALLOWANCE)
        val currentAccuracy = _horizontalAccuracy?.takeIf { it > 0f } ?: DEFAULT_ACCURACY
        val maxDistance = speedLimit * seconds + DISTANCE_UNCERTAINTY_FACTOR * hypot(currentAccuracy, newAccuracy)
        if (distance > maxDistance) {
            logRejectedReading("implausible movement (max allowed: ${maxDistance.safeRoundPlaces(1)}m)")
            return false
        }

        return true
    }

    private fun logRejectedReading(reason: String) {
        logger.debug(TAG, "[$diagnosticId] Location Rejected: $reason, ${describeNewReading()}")
    }

    private fun logAcceptedReading(reason: String) {
        logger.debug(TAG, "[$diagnosticId] Location Accepted: $reason, ${describeNewReading()}")
    }

    private fun describeNewReading(): String {
        return "Time Delta: ${
            Duration.between(_time, baseGPS.time).toMillis()
        }ms, Distance: ${
            _location.distanceTo(baseGPS.location).safeRoundPlaces(1)
        }m, Accuracy: ${_horizontalAccuracy?.safeRoundPlaces(1)}m -> ${
            baseGPS.horizontalAccuracy?.safeRoundPlaces(1)
        }m, Age: ${
            Duration.between(_time, Instant.now()).toMillis()
        }ms"
    }

    companion object {
        // The min and max speed for location filtering (m/s)
        private const val MIN_SPEED_ALLOWANCE = 5f
        private const val MAX_SPEED_ALLOWANCE = 55f

        // A factor to scale the max distance error of new readings
        private const val DISTANCE_UNCERTAINTY_FACTOR = 2.5f
        private const val DEFAULT_ACCURACY = 50f

        // Readings with this accuracy are too poor to accept, wait for another reading
        private const val MAX_ACCEPTABLE_ACCURACY = 150f
        private const val TAG = "FilteredGPS"

        // This is used to distinguish instances of this class in the logs, since there can be multiple instances of this class at once
        private val nextDiagnosticId = AtomicInteger(1)

    }
}
