package com.kylecorry.trail_sense.shared.sensors.gps.modules

import android.os.SystemClock
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.settings.infrastructure.IGPSPreferences
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.safeRoundPlaces
import com.kylecorry.trail_sense.shared.sensors.gps.GPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData

/**
 * Rejects readings which do not meet the user's accuracy requirement for a bounded time.
 * After the wait, the next reading can update the location even if its accuracy is still poor.
 */
class MinimumAccuracyGPSModule(
    private val prefs: IGPSPreferences = getAppService<UserPreferences>().gps,
    private val logger: Logger = getAppService(),
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime
) : GPSModule {
    private var rejectionStartMillis: Long? = null

    override fun start(data: ModularGPSData) {
        rejectionStartMillis = null
    }

    override fun stop(data: ModularGPSData) {
        rejectionStartMillis = null
    }

    override fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
        val requirement = prefs.accuracyRequirement
        val minAccuracy = requirement.minAccuracy

        // An unknown accuracy can't be judged against the requirement
        val accuracy = newData.horizontalAccuracy?.takeIf { it > 0f }

        if (minAccuracy == null || accuracy == null || accuracy <= minAccuracy) {
            rejectionStartMillis = null
            return true
        }

        val now = elapsedRealtime()
        val start = rejectionStartMillis ?: now.also { rejectionStartMillis = it }
        val elapsedMillis = now - start
        val maxAccuracyWait = requirement.maxAccuracyWait
        if (maxAccuracyWait != null && elapsedMillis >= maxAccuracyWait.toMillis()) {
            logger.debug(
                TAG,
                "Location Accepted: accuracy wait of ${maxAccuracyWait.seconds}s reached, " +
                        "Accuracy: ${accuracy.safeRoundPlaces(1)}m"
            )
            rejectionStartMillis = null
            return true
        }

        logger.debug(
            TAG,
            "Location Rejected: accuracy requirement (${requirement.name}) not met, " +
                    "Accuracy: ${accuracy.safeRoundPlaces(1)}m > ${minAccuracy.safeRoundPlaces(1)}m, " +
                    "Waiting: ${elapsedMillis}ms"
        )
        return false
    }

    companion object {
        private const val TAG = "MinimumAccuracyGPSModule"
    }
}
