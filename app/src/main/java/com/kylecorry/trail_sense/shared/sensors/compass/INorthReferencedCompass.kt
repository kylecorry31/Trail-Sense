package com.kylecorry.trail_sense.shared.sensors.compass

import com.kylecorry.andromeda.sense.compass.ICompass

interface INorthReferencedCompass : ICompass {
    /**
     * Whether the bearing is relative to true north. When false, the declination is not applied to the bearing.
     */
    val useTrueNorth: Boolean
}
