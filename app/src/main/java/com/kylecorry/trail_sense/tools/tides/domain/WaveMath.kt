package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.sol.math.Vector2
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min

object WaveMath {

    fun interpolateWave(first: Vector2, second: Vector2): Wave {
        val period = second.x - first.x
        val deltaY = abs(first.y - second.y)
        val verticalShift = deltaY / 2 + min(first.y, second.y)
        val frequency = PI / period
        val amplitude = (if (first.y > second.y) 1 else -1) * deltaY / 2
        return Wave(amplitude, frequency.toFloat(), first.x, verticalShift)
    }

}