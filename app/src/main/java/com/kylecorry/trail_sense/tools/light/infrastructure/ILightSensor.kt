package com.kylecorry.trail_sense.tools.light.infrastructure

import com.kylecorry.trailsensecore.infrastructure.sensors.ISensor

interface ILightSensor: ISensor {
    val illuminance: Float
}