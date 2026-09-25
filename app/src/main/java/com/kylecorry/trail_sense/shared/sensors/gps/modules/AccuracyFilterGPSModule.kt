package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.andromeda.core.time.SystemTimeProvider
import com.kylecorry.andromeda.core.time.TimeProvider
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.settings.infrastructure.IGPSPreferences
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.safeRoundPlaces
import com.kylecorry.trail_sense.shared.sensors.gps.GPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import com.kylecorry.trail_sense.shared.sensors.gps.durationSince
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

    private val breaker = AccuracyCircuitBreaker(timeProvider)
    private var bestReading: ModularGPSData? = null

    // Rejections are summarized rather than logged individually because they can occur on every fix
    private var rejectedCount = 0
    private var firstRejectedTimeNanos = 0L
    private var lastRejectedTimeNanos = 0L

    override suspend fun start(data: ModularGPSData) {
        breaker.resetIfAccepted(data.id)
        bestReading = null
        rejectedCount = 0
    }

    override suspend fun stop(data: ModularGPSData) {
        breaker.resetIfAccepted(data.id)
        breaker.pause()
        bestReading = null
        if (rejectedCount > 0) {
            logger.debug(TAG, "Stopped after ${describeRejections()}")
            rejectedCount = 0
        }
    }

    override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
        if (newData.durationSince(previousData) <= Duration.ZERO) return true

        if (previousData.location == Coordinate.zero) return true

        if (previousData.isTimedOut) {
            logAccepted("timed out", newData)
            return true
        }

        val filter = prefs.accuracyFilter
        val minAccuracy = filter.minAccuracy

        // An unknown accuracy can't be judged against the filter
        val accuracy = newData.horizontalAccuracy?.takeIf { it > 0f }

        if (minAccuracy == null || accuracy == null || accuracy <= minAccuracy) {
            logAccepted("met filter", newData)
            reset()
            return true
        }

        breaker.resetIfAccepted(previousData.id)

        // The breaker tripped without the candidate being accepted, so just take whatever is next
        if (breaker.isOpen) {
            logAccepted("breaker open", newData)
            return true
        }

        // Discard the retained fix once the pipeline has accepted it or a newer fix.
        if (bestReading?.let { it.durationSince(previousData) <= Duration.ZERO } == true) {
            bestReading = null
        }

        // Only consider a recent best reading
        bestReading = bestReading?.takeIf {
            newData.durationSince(it) <= MAX_RETAINED_FIX_AGE
        }
        val best = bestReading
        val candidate = if (best == null || accuracy <= (best.horizontalAccuracy ?: Float.POSITIVE_INFINITY)) {
            ModularGPSData().also { newData.copyInto(it) }
        } else {
            best
        }
        bestReading = candidate

        val maxAccuracyWait = filter.maxAccuracyWait
        if (maxAccuracyWait != null && breaker.tripIfTimedOut(
                maxAccuracyWait.toMillis(),
                previousId = previousData.id
            )
        ) {
            val ageMillis = newData.durationSince(candidate).toMillis()
            candidate.copyInto(newData)
            logAccepted("breaker tripped, best reading ${ageMillis}ms old", newData)
            return true
        }

        logRejected(newData, accuracy, minAccuracy)
        return false
    }

    private fun logRejected(newData: ModularGPSData, accuracy: Float, minAccuracy: Float) {
        if (rejectedCount == 0) {
            firstRejectedTimeNanos = newData.eventTimeElapsedNanos
            logger.debug(TAG, "Rejecting: ${accuracy.safeRoundPlaces(1)}m > ${minAccuracy.safeRoundPlaces(1)}m")
        }
        rejectedCount++
        lastRejectedTimeNanos = newData.eventTimeElapsedNanos
    }

    private fun logAccepted(reason: String, newData: ModularGPSData) {
        if (rejectedCount == 0) {
            return
        }
        logger.debug(
            TAG,
            "Accept ($reason): ${newData.horizontalAccuracy?.safeRoundPlaces(1)}m after ${describeRejections()}"
        )
        rejectedCount = 0
    }

    private fun describeRejections(): String {
        val seconds = Duration.ofNanos(lastRejectedTimeNanos - firstRejectedTimeNanos).seconds
        return "$rejectedCount rejected readings over ${seconds}s"
    }

    private fun reset() {
        breaker.reset()
        bestReading = null
    }

    companion object {
        private const val TAG = "AccuracyFilterGPSModule"
        private val MAX_RETAINED_FIX_AGE: Duration = Duration.ofSeconds(5)
    }
}
