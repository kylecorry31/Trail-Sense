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
import java.time.Duration

/**
 * Rejects readings which do not meet the user's accuracy filter for a bounded time.
 * After the wait, the most accurate recent reading seen during the wait can update the location (if available, otherwise the next reading).
 */
class AccuracyFilterGPSModule(
    private val prefs: IGPSPreferences = getAppService<UserPreferences>().gps,
    private val logger: Logger = getAppService(),
    timeProvider: TimeProvider = SystemTimeProvider()
) : GPSModule {

    private val rejectionTracker = GPSRejectionTracker(timeProvider)
    private var bestReading: ModularGPSData? = null

    override suspend fun start(data: ModularGPSData) {
        reset()
    }

    override suspend fun stop(data: ModularGPSData) {
        reset()
    }

    override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
        if (newData.time <= previousData.time) return true

        val filter = prefs.accuracyFilter
        val minAccuracy = filter.minAccuracy

        // An unknown accuracy can't be judged against the filter
        val accuracy = newData.horizontalAccuracy?.takeIf { it > 0f }

        if (minAccuracy == null || accuracy == null || accuracy <= minAccuracy) {
            reset()
            return true
        }

        // It hit a timeout and the candidate wasn't accepted, so just take whatever is next
        if (rejectionTracker.isAwaitingAcceptance(previousData.time)) {
            logger.debug(TAG, "Location Accepted: awaiting pipeline acceptance for fallback")
            return true
        }

        // Discard the retained fix once the pipeline has accepted it or a newer fix.
        if (bestReading?.time?.let { it <= previousData.time } == true) {
            bestReading = null
        }

        // Only consider a recent best reading
        bestReading = bestReading?.takeIf {
            Duration.between(it.time, newData.time) <= MAX_RETAINED_FIX_AGE
        }
        val best = bestReading
        if (best == null || accuracy <= (best.horizontalAccuracy ?: Float.POSITIVE_INFINITY)) {
            bestReading = ModularGPSData().also { newData.copyInto(it) }
        }
        val candidate = bestReading ?: return false

        val maxAccuracyWait = filter.maxAccuracyWait
        if (maxAccuracyWait != null && rejectionTracker.isTimedOut(
                maxAccuracyWait.toMillis(),
                newFixTime = candidate.time
            )
        ) {
            candidate.copyInto(newData)
            logger.debug(
                TAG,
                "Location Accepted: accuracy wait of ${maxAccuracyWait.seconds}s reached, " +
                        "Accuracy: ${candidate.horizontalAccuracy?.safeRoundPlaces(1)}m"
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

    private fun reset() {
        rejectionTracker.reset()
        bestReading = null
    }

    companion object {
        private const val TAG = "AccuracyFilterGPSModule"
        private val MAX_RETAINED_FIX_AGE: Duration = Duration.ofSeconds(2)
    }
}
