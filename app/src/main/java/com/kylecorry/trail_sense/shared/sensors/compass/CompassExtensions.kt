package com.kylecorry.trail_sense.shared.sensors.compass

import com.kylecorry.sol.units.Bearing

fun INorthReferencedCompass.getTrueNorthBearing(): Bearing {
    return if (useTrueNorth) bearing else bearing.withDeclination(declination)
}
