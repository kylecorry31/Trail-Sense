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

    override fun start(data: ModularGPSData) {
        rejectionTracker.reset()
    }

    override fun stop(data: ModularGPSData) {
        rejectionTracker.reset()
    }

    override fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
        if (rejectionTracker.isAwaitingAcceptance(previousData.time)) {
            logger.debug(TAG, "Location Accepted: awaiting pipeline acceptance for fallback")
            return true
        }

        val satelliteCount = newData.satellites

        // If satellite count is null, then the phone doesn't support satellite count
        if (satelliteCount == null || !prefs.requiresSatellites || satelliteCount >= 4) {
            rejectionTracker.reset()
            return true
        }

        if (rejectionTracker.isTimedOut(
                maxSatelliteWait.toMillis(),
                newFixTime = newData.time
            )
        ) {
            logger.debug(
                TAG,
                "Location Accepted: satellite wait of ${maxSatelliteWait.seconds}s reached, " +
                        "Satellites: $satelliteCount"
            )
            return true
        }

        logger.debug(TAG, "Location Rejected: not enough satellites ($satelliteCount)")
        return false
    }

    companion object {
        private const val TAG = "SatelliteFixFilterGPSModule"
        private val maxSatelliteWait = Duration.ofSeconds(5)
    }
}
