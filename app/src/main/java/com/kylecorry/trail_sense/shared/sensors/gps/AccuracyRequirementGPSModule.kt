package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.settings.infrastructure.IGPSPreferences
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.safeRoundPlaces

/**
 * Rejects readings which do not meet the user's accuracy requirement. Consecutive rejections are
 * capped so the location can still update when the GPS never reaches the requested accuracy.
 */
class AccuracyRequirementGPSModule(
    private val prefs: IGPSPreferences = getAppService<UserPreferences>().gps,
    private val logger: Logger = getAppService()
) : GPSModule {
    private var rejectionCount = 0

    override fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
        val requirement = prefs.accuracyRequirement
        val minAccuracy = requirement.minAccuracy ?: return true

        // An unknown accuracy can't be judged against the requirement
        val accuracy = newData.horizontalAccuracy?.takeIf { it > 0f } ?: return true

        if (accuracy <= minAccuracy) {
            rejectionCount = 0
            return true
        }

        val maxRejectionCount = requirement.maxRejectionCount
        if (maxRejectionCount != null && rejectionCount >= maxRejectionCount) {
            logger.debug(
                TAG,
                "Location Accepted: $rejectionCount readings already rejected, " +
                        "Accuracy: ${accuracy.safeRoundPlaces(1)}m"
            )
            rejectionCount = 0
            return true
        }

        rejectionCount++
        logger.debug(
            TAG,
            "Location Rejected: accuracy requirement (${requirement.name}) not met, " +
                    "Accuracy: ${accuracy.safeRoundPlaces(1)}m > ${minAccuracy.safeRoundPlaces(1)}m, " +
                    "Rejections: $rejectionCount"
        )
        return false
    }

    companion object {
        private const val TAG = "FilteredGPS"
    }
}
