package com.kylecorry.trail_sense.shared.sensors.odometer

import com.kylecorry.andromeda.core.sensors.ISensor
import com.kylecorry.andromeda.core.units.Distance

interface IOdometer: ISensor {
    val distance: Distance
}