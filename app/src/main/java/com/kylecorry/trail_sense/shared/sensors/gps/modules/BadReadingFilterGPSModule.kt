package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.safeRoundPlaces
import com.kylecorry.trail_sense.shared.sensors.gps.GPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import java.time.Duration
import java.time.Instant

/**
 * Rejects readings which are clearly erroneous.
 */
class BadReadingFilterGPSModule(
    private val logger: Logger = getAppService()
) : GPSModule {

    override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
        if (!newData.hasValidReading) {
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
        if (newData.time.isBefore(previousData.time)) {
            logRejectedReading("older", previousData, newData)
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
            "Rejected: $reason, ${describeNewReading(previousData, newData)}"
        )
    }

    private fun logAcceptedReading(
        reason: String,
        previousData: ModularGPSData,
        newData: ModularGPSData
    ) {
        logger.debug(
            TAG,
            "Accepted: $reason, ${describeNewReading(previousData, newData)}"
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
        private const val TAG = "BadReadingFilterGPSModule"
    }
}
