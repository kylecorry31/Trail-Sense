package com.kylecorry.trail_sense.tools.pedometer.infrastructure.odometer

import com.kylecorry.andromeda.core.sensors.ISensor
import com.kylecorry.sol.units.Distance

interface IOdometer: ISensor {
    val distance: Distance
}