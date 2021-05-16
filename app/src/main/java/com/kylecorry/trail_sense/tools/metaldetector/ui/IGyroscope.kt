package com.kylecorry.trail_sense.tools.metaldetector.ui

import com.kylecorry.trailsensecore.domain.math.Vector3
import com.kylecorry.trailsensecore.infrastructure.sensors.ISensor

interface IGyroscope: ISensor {
    val rawRotation: FloatArray
    val rotation: Vector3
    fun calibrate()
}