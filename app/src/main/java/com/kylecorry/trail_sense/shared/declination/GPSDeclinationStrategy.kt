package com.kylecorry.trail_sense.shared.declination

import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.sol.science.geology.IGeologyService

class GPSDeclinationStrategy(
    private val gps: IGPS,
    private val geology: IGeologyService = GeologyService()
) : IDeclinationStrategy {
    override fun getDeclination(): Float {
        return geology.getMagneticDeclination(gps.location, gps.altitude)
    }
}