package com.kylecorry.trail_sense.shared.declination

import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.sol.science.geology.GeologyService

class GPSDeclinationStrategy(private val gps: IGPS) : IDeclinationStrategy {

    private val geology = GeologyService()

    override fun getDeclination(): Float {
        return geology.getMagneticDeclination(gps.location, gps.altitude)
    }
}