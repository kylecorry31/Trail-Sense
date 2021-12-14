package com.kylecorry.trail_sense.tools.clinometer.infrastructure

import com.kylecorry.andromeda.core.sensors.ISensor

interface IClinometer: ISensor {
    val angle: Float
    val incline: Float
}