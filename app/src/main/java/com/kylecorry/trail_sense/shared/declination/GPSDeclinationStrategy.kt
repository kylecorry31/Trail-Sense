package com.kylecorry.trail_sense.shared.declination

import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.science.geology.IGeologyService
import com.kylecorry.sol.units.Coordinate

class GPSDeclinationStrategy(
    private val gps: IGPS,
    private val geology: IGeologyService = Geology,
    private val cacheRadiusMeters: Float = 1000f
) : IDeclinationStrategy {

    private var cachedDeclination: Float? = null
    private var cachedLocation: Coordinate? = null
    private val lock = Any()

    override fun getDeclination(): Float {
        synchronized(lock) {
            if (cachedLocation == null || cachedLocation!!.distanceTo(gps.location) > cacheRadiusMeters) {
                cachedLocation = gps.location
                cachedDeclination = geology.getGeomagneticDeclination(gps.location, gps.altitude)
            }
            return cachedDeclination!!
        }
    }
}