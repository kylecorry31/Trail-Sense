package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.sensors.gps.GPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData

class SameFixGPSModule : GPSModule {

    override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
        if (newData.id != previousData.id || previousData.location == Coordinate.zero) {
            return true
        }

        previousData.copyInto(newData)

        return true
    }
}
