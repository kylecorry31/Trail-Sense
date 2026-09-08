package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.sensors.gps.GPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData

/**
 * Restores the values already published for a fix when it is seen again. The GPS reports satellite
 * and NMEA updates using the last fix, and those readings must not be reprocessed by the modules
 * which follow.
 */
class SameFixGPSModule : GPSModule {

    override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
        if (newData.id != previousData.id || previousData.location == Coordinate.zero) {
            return true
        }

        // Satellites and the MSL altitude come from listeners which run independently of a fix
        val satellites = newData.satellites
        val satelliteDetails = newData.satelliteDetails
        val mslAltitude = newData.mslAltitude

        previousData.copyInto(newData)

        newData.satellites = satellites
        newData.satelliteDetails = satelliteDetails
        newData.mslAltitude = mslAltitude

        return true
    }
}
