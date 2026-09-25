package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.andromeda.core.time.SystemTimeProvider
import com.kylecorry.andromeda.core.time.TimeProvider
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.safeRoundPlaces
import com.kylecorry.trail_sense.shared.sensors.gps.GPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import com.kylecorry.trail_sense.shared.sensors.gps.age
import com.kylecorry.trail_sense.shared.sensors.gps.durationSince
import java.time.Duration

/**
 * Rejects readings which are clearly erroneous.
 */
class BadReadingFilterGPSModule(
    private val logger: Logger = getAppService(),
    private val timeProvider: TimeProvider = SystemTimeProvider()
) : GPSModule {

    override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
        if (!newData.hasValidReading) {
            return false
        }

        if (previousData.location == Coordinate.zero) {
            logger.debug(TAG, "Accepted: no previous reading, ${describeNewReading(previousData, newData)}")
            return true
        }

        // The current reading is somehow in the future, so just accept a new reading (prevents stuck readings)
        if (previousData.age(timeProvider) < FUTURE_TOLERANCE) {
            logger.warn(
                TAG,
                "Accepted: last reading is in the future, ${describeNewReading(previousData, newData)}"
            )
            return true
        }

        // The new reading is older than the current one, so reject it
        if (newData.durationSince(previousData).isNegative) {
            logger.debug(TAG, "Rejected: older, ${describeNewReading(previousData, newData)}")
            return false
        }

        return true
    }

    private fun describeNewReading(
        previousData: ModularGPSData,
        newData: ModularGPSData
    ): String {
        return "Time Delta: ${
            newData.durationSince(previousData).toMillis()
        }ms, Distance: ${
            previousData.location.distanceTo(newData.location).safeRoundPlaces(1)
        }m, Accuracy: ${previousData.horizontalAccuracy?.safeRoundPlaces(1)}m -> ${
            newData.horizontalAccuracy?.safeRoundPlaces(1)
        }m, Age: ${
            previousData.age(timeProvider).toMillis()
        }ms"
    }

    companion object {
        private const val TAG = "BadReadingFilterGPSModule"
        private val FUTURE_TOLERANCE: Duration = Duration.ofMillis(-500)
    }
}
