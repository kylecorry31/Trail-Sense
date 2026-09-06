package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.settings.infrastructure.IGPSPreferences
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.SystemTimeProvider
import com.kylecorry.trail_sense.shared.andromeda_temp.TimeProvider
import com.kylecorry.trail_sense.shared.andromeda_temp.TimeoutTracker
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.sensors.gps.GPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import java.time.Duration

class SatelliteFixFilterGPSModule(
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
        val satelliteCount = newData.satellites

        // If satellite count is null, then the phone doesn't support satellite count
        if (satelliteCount == null || !prefs.requiresSatellites || satelliteCount >= 4) {
            rejectionTimeout.reset()
            return true
        }

        if (rejectionTimeout.isTimedOut(
                maxSatelliteWait.toMillis(),
                startIfNotStarted = true
            )
        ) {
            logger.debug(
                TAG,
                "Location Accepted: satellite wait of ${maxSatelliteWait.seconds}s reached, " +
                        "Satellites: $satelliteCount"
            )
            rejectionTimeout.reset()
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
