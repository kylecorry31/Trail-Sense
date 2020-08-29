package com.kylecorry.trail_sense.shared.sensors.declination

import com.kylecorry.trail_sense.shared.sensors.ISensor

interface IDeclinationProvider: ISensor {

    val declination: Float

}