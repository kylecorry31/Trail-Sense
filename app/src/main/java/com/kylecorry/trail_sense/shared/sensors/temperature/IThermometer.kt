package com.kylecorry.trail_sense.shared.sensors.temperature

import com.kylecorry.trail_sense.shared.sensors.ISensor

interface IThermometer: ISensor {
    val temperature: Float
}