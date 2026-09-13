package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.GeoidService
import com.kylecorry.trail_sense.shared.sensors.gps.GPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData

class MeanSeaLevelGPSModule(
    private val geoidService: GeoidService = getAppService()
) : GPSModule {
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

        newData.altitude -= getGeoidOffset(newData.location)

        return true
    }

    private suspend fun getGeoidOffset(location: Coordinate): Float {
        val lastGeoidLocation = geoidLocation

        if (lastGeoidLocation == null || !geoidService.isSameGeoid(lastGeoidLocation, location)) {
            geoidOffset = geoidService.getGeoid(location)
            geoidLocation = location
        }

        return geoidOffset
    }
}
