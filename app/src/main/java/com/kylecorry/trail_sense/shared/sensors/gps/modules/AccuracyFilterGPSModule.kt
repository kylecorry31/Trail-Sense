package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.settings.infrastructure.IGPSPreferences
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.SystemTimeProvider
import com.kylecorry.trail_sense.shared.andromeda_temp.TimeProvider
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.safeRoundPlaces
import com.kylecorry.trail_sense.shared.sensors.gps.GPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData

/**
 * Rejects readings which do not meet the user's accuracy filter for a bounded time.
 * After the wait, the next reading can update the location even if its accuracy is still poor.
 */
class AccuracyFilterGPSModule(
    private val prefs: IGPSPreferences = getAppService<UserPreferences>().gps,
    private val logger: Logger = getAppService(),
    timeProvider: TimeProvider = SystemTimeProvider()
) : GPSModule {

    private val rejectionTracker = GPSRejectionTracker(timeProvider)

    override suspend fun start(data: ModularGPSData) {
        rejectionTracker.reset()
    }

    override suspend fun stop(data: ModularGPSData) {
        rejectionTracker.reset()
    }

    override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
        if (rejectionTracker.isAwaitingAcceptance(previousData.time)) {
            logger.debug(TAG, "Location Accepted: awaiting pipeline acceptance for fallback")
            return true
        }

        val filter = prefs.accuracyFilter
        val minAccuracy = filter.minAccuracy

        // An unknown accuracy can't be judged against the filter
        val accuracy = newData.horizontalAccuracy?.takeIf { it > 0f }

        if (minAccuracy == null || accuracy == null || accuracy <= minAccuracy) {
            rejectionTracker.reset()
            return true
        }

        val maxAccuracyWait = filter.maxAccuracyWait
        if (maxAccuracyWait != null && rejectionTracker.isTimedOut(
                maxAccuracyWait.toMillis(),
                newFixTime = newData.time
            )
        ) {
            logger.debug(
                TAG,
                "Location Accepted: accuracy wait of ${maxAccuracyWait.seconds}s reached, " +
                        "Accuracy: ${accuracy.safeRoundPlaces(1)}m"
            )
            return true
        }

        logger.debug(
            TAG,
            "Location Rejected: accuracy filter (${filter.name}) not met, " +
                    "Accuracy: ${accuracy.safeRoundPlaces(1)}m > ${minAccuracy.safeRoundPlaces(1)}m"
        )
        return false
    }

    companion object {
        private const val TAG = "AccuracyFilterGPSModule"
    }
}
