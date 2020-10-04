package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.trailsensecore.domain.math.Vector3
import com.kylecorry.trailsensecore.infrastructure.sensors.ISensor

interface IOrientationSensor: ISensor {

    val orientation: Vector3

}