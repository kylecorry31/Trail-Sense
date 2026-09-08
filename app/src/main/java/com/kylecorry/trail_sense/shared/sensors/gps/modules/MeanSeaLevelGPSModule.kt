package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.settings.infrastructure.IGPSPreferences
import com.kylecorry.trail_sense.shared.GeoidService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.gps.GPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData

class MeanSeaLevelGPSModule(
    private val prefs: IGPSPreferences = getAppService<UserPreferences>().gps,
    private val geoidService: GeoidService = getAppService()
) : GPSModule {
    private var mslOffset = 0f

    private var geoidOffset = 0f
    private var geoidLocation: Coordinate? = null

    override suspend fun update(
        previousData: ModularGPSData,
        newData: ModularGPSData
    ): Boolean {
        // The altitude of a repeated fix was already corrected
        if (newData.id == previousData.id) {
            return true
        }

        val newMSLOffset = newData.altitude - (newData.mslAltitude ?: newData.altitude)
        if (newMSLOffset != 0f) {
            mslOffset = newMSLOffset
        }

        newData.altitude -= getGeoidOffset(newData.location)

        return true
    }

    private suspend fun getGeoidOffset(location: Coordinate): Float {
        if (prefs.useNMEA && mslOffset != 0f) {
            return mslOffset
        }

        val lastGeoidLocation = geoidLocation

        if (lastGeoidLocation == null || !geoidService.isSameGeoid(lastGeoidLocation, location)) {
            geoidOffset = geoidService.getGeoid(location)
            geoidLocation = location
        }

        return geoidOffset
    }
}
