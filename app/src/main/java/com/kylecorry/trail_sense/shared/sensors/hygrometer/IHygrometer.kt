package com.kylecorry.trail_sense.shared.sensors.hygrometer

import com.kylecorry.trail_sense.shared.sensors.ISensor

interface IHygrometer: ISensor {
    val humidity: Float
}