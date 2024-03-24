package com.kylecorry.trail_sense.shared.declination

import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.science.geology.IGeologyService
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.hooks.HookTriggers

class GPSDeclinationStrategy(
    private val gps: IGPS,
    private val geology: IGeologyService = Geology,
    cacheRadiusMeters: Float = 1000f
) : IDeclinationStrategy {

    private val hooks = Hooks()
    private val triggers = HookTriggers()
    private val cacheRadius = Distance.meters(cacheRadiusMeters)

    override fun getDeclination(): Float {
        return hooks.memo(
            "declination",
            triggers.distance("declination", gps.location, cacheRadius, highAccuracy = false)
        ) {
            geology.getGeomagneticDeclination(gps.location, gps.altitude)
        }
    }
}