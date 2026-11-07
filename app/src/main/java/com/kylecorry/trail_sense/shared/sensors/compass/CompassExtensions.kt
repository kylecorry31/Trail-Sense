package com.kylecorry.trail_sense.shared.sensors.compass

import com.kylecorry.sol.units.Bearing

/**
 * Gets the bearing relative to true north, even when the compass is set to magnetic north
 */
fun INorthReferencedCompass.getTrueNorthBearing(): Bearing {
    return if (useTrueNorth) bearing else bearing.withDeclination(declination)
}
