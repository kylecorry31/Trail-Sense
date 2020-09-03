package com.kylecorry.trail_sense.tools.inclinometer.domain

import com.kylecorry.trail_sense.shared.domain.Vector3
import kotlin.math.atan2

object InclinationCalculator {

    fun calculate(gravity: Vector3): Float {
        var angle = Math.toDegrees(atan2(gravity.y.toDouble(), gravity.x.toDouble())).toFloat()

        if (angle > 90) {
            angle = 180 - angle
        }

        if (angle < -90) {
            angle = -180 - angle
        }

        return angle
    }

}