package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.settings.infrastructure.IGPSPreferences
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.SystemTimeProvider
import com.kylecorry.trail_sense.shared.andromeda_temp.TimeProvider
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.sensors.gps.GPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import java.time.Duration

class SatelliteFixFilterGPSModule(
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
        if (newData.time <= previousData.time) return true

        val satelliteCount = newData.satellites

        if (rejectionTracker.isAwaitingAcceptance(previousData.time)) {
            logger.debug(TAG, "Accept (timeout fallback): $satelliteCount satellites")
            return true
        }

        // If satellite count is null, then the phone doesn't support satellite count
        if (satelliteCount == null || !prefs.requiresSatelliteCount || satelliteCount >= 4) {
            rejectionTracker.reset()
            return true
        }

        if (rejectionTracker.isTimedOut(
                maxSatelliteWait.toMillis(),
                newFixTime = newData.time
            )
        ) {
            logger.debug(TAG, "Accept (timeout): $satelliteCount satellites")
            return true
        }

        logger.debug(TAG, "Reject: $satelliteCount satellites")
        return false
    }

    companion object {
        private const val TAG = "SatelliteFixFilterGPSModule"
        private val maxSatelliteWait = Duration.ofSeconds(5)
    }
}
