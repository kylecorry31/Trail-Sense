package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.trail_sense.shared.domain.Vector3

interface IAccelerometer: ISensor {
    val acceleration: Vector3
}