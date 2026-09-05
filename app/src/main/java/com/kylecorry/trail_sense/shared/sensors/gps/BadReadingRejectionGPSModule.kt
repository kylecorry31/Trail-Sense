package com.kylecorry.trail_sense.shared.sensors.gps

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

/**
 * Rejects readings which are clearly erroneous.
 */
class BadReadingRejectionGPSModule(
    private val userPrefs: UserPreferences = getAppService(),
    private val logger: Logger = getAppService()
) : GPSModule {
    private val diagnosticId = nextDiagnosticId.getAndIncrement()

    override fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
        if (!newData.hasValidReading) {
            return false
        }

        if (!userPrefs.filterLocationReadings) {
            return true
        }

        // The new reading is so inaccurate that it can't be useful
        val newAccuracy = newData.horizontalAccuracy?.takeIf { it > 0f } ?: DEFAULT_ACCURACY
        if (newAccuracy > MAX_ACCEPTABLE_ACCURACY) {
            logRejectedReading("poor accuracy", previousData, newData)
            return false
        }

        // If satellite count is null, then the phone doesn't support satellite count
        val satelliteCount = newData.satellites
        val hasFix = satelliteCount == null || !userPrefs.requiresSatellites || satelliteCount >= 4
        if (!hasFix) {
            logRejectedReading("not enough satellites ($satelliteCount)", previousData, newData)
            return false
        }

        if (previousData.location == Coordinate.zero) {
            logAcceptedReading("no previous reading", previousData, newData)
            return true
        }

        // The current reading is somehow in the future, so just accept a new reading (prevents stuck readings)
        val isLastTimeInFuture = previousData.time.isAfter(Instant.now().plusMillis(500))
        if (isLastTimeInFuture) {
            logAcceptedReading("last reading is in the future", previousData, newData)
            return true
        }

        // The new reading is older than the current one, so reject it
        val timeDelta = Duration.between(previousData.time, newData.time)
        if (timeDelta < Duration.ZERO) {
            logRejectedReading("older", previousData, newData)
            return false
        }

        // The reading is stale, a new one is needed so accept it even if it is not great
        if (timeDelta > STALE_READING_DURATION) {
            logAcceptedReading("stale reading", previousData, newData)
            return true
        }

        // The new reading is farther away than the user could have possibly traveled since the last one
        val seconds = timeDelta.toMillis() / 1000f
        val distance = previousData.location.distanceTo(newData.location)
        val speedLimit = previousData.speed.value.real(0f)
            .coerceAtLeast(newData.speed.value.real(0f))
            .coerceIn(MIN_SPEED_ALLOWANCE, MAX_SPEED_ALLOWANCE)
        val currentAccuracy = previousData.horizontalAccuracy?.takeIf { it > 0f } ?: DEFAULT_ACCURACY
        val maxDistance =
            speedLimit * seconds + DISTANCE_UNCERTAINTY_FACTOR * hypot(currentAccuracy, newAccuracy)
        if (distance > maxDistance) {
            logRejectedReading(
                "implausible movement (max allowed: ${maxDistance.safeRoundPlaces(1)}m)",
                previousData,
                newData
            )
            return false
        }

        return true
    }

    private fun logRejectedReading(
        reason: String,
        previousData: ModularGPSData,
        newData: ModularGPSData
    ) {
        logger.debug(
            TAG,
            "[$diagnosticId] Location Rejected: $reason, ${describeNewReading(previousData, newData)}"
        )
    }

    private fun logAcceptedReading(
        reason: String,
        previousData: ModularGPSData,
        newData: ModularGPSData
    ) {
        logger.debug(
            TAG,
            "[$diagnosticId] Location Accepted: $reason, ${describeNewReading(previousData, newData)}"
        )
    }

    private fun describeNewReading(
        previousData: ModularGPSData,
        newData: ModularGPSData
    ): String {
        return "Time Delta: ${
            Duration.between(previousData.time, newData.time).toMillis()
        }ms, Distance: ${
            previousData.location.distanceTo(newData.location).safeRoundPlaces(1)
        }m, Accuracy: ${previousData.horizontalAccuracy?.safeRoundPlaces(1)}m -> ${
            newData.horizontalAccuracy?.safeRoundPlaces(1)
        }m, Age: ${
            Duration.between(previousData.time, Instant.now()).toMillis()
        }ms"
    }

    companion object {
        // The min and max speed for location filtering (m/s)
        private const val MIN_SPEED_ALLOWANCE = 2f
        private const val MAX_SPEED_ALLOWANCE = 40f

        // A factor to scale the max distance error of new readings
        private const val DISTANCE_UNCERTAINTY_FACTOR = 2.5f
        private const val DEFAULT_ACCURACY = 50f

        // Readings with this accuracy are too poor to accept, wait for another reading
        private const val MAX_ACCEPTABLE_ACCURACY = 150f
        private val STALE_READING_DURATION = Duration.ofMinutes(2)
        private const val TAG = "FilteredGPS"

        // This is used to distinguish instances of this class in the logs, since there can be multiple instances of this class at once
        private val nextDiagnosticId = AtomicInteger(1)
    }
}
