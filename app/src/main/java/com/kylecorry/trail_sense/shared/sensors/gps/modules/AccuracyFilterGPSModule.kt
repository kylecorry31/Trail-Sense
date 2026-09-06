package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.settings.infrastructure.IGPSPreferences
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.SystemTimeProvider
import com.kylecorry.trail_sense.shared.andromeda_temp.TimeProvider
import com.kylecorry.trail_sense.shared.andromeda_temp.TimeoutTracker
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.safeRoundPlaces
import com.kylecorry.trail_sense.shared.sensors.gps.GPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData

/**
 * Rejects readings which do not meet the user's accuracy requirement for a bounded time.
 * After the wait, the next reading can update the location even if its accuracy is still poor.
 */
class AccuracyFilterGPSModule(
    private val prefs: IGPSPreferences = getAppService<UserPreferences>().gps,
    private val logger: Logger = getAppService(),
    timeProvider: TimeProvider = SystemTimeProvider()
) : GPSModule {

    private val rejectionTimeout = TimeoutTracker(timeProvider)

    override fun start(data: ModularGPSData) {
        rejectionTimeout.reset()
    }

    override fun stop(data: ModularGPSData) {
        rejectionTimeout.reset()
    }

    override fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
        val requirement = prefs.accuracyRequirement
        val minAccuracy = requirement.minAccuracy

        // An unknown accuracy can't be judged against the requirement
        val accuracy = newData.horizontalAccuracy?.takeIf { it > 0f }

        if (minAccuracy == null || accuracy == null || accuracy <= minAccuracy) {
            rejectionTimeout.reset()
            return true
        }

        val maxAccuracyWait = requirement.maxAccuracyWait
        if (maxAccuracyWait != null && rejectionTimeout.isTimedOut(
                maxAccuracyWait.toMillis(),
                startIfNotStarted = true
            )
        ) {
            logger.debug(
                TAG,
                "Location Accepted: accuracy wait of ${maxAccuracyWait.seconds}s reached, " +
                        "Accuracy: ${accuracy.safeRoundPlaces(1)}m"
            )
            rejectionTimeout.reset()
            return true
        }

        logger.debug(
            TAG,
            "Location Rejected: accuracy requirement (${requirement.name}) not met, " +
                    "Accuracy: ${accuracy.safeRoundPlaces(1)}m > ${minAccuracy.safeRoundPlaces(1)}m"
        )
        return false
    }

    companion object {
        private const val TAG = "AccuracyFilterGPSModule"
    }
}
