package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.sol.math.SolMath.power
import com.kylecorry.sol.math.Vector2
import kotlin.math.*

object WaveMath {

    /**
     * Connects the first and second points using a wave (Cosine)
     */
    fun connect(first: Vector2, second: Vector2): Wave {
        val period = second.x - first.x
        val deltaY = abs(first.y - second.y)
        val verticalShift = deltaY / 2 + min(first.y, second.y)
        val frequency = PI / period
        val amplitude = (if (first.y > second.y) 1 else -1) * deltaY / 2
        return Wave(amplitude, frequency.toFloat(), first.x, verticalShift)
    }

    /**
     * Connects the first and second points using a wave (Cosine), trying to approximate the frequency provided
     */
    fun connect(first: Vector2, second: Vector2, approximateFrequency: Float): Wave {
        val period = second.x - first.x
        val deltaY = abs(first.y - second.y)
        val verticalShift = deltaY / 2 + min(first.y, second.y)
        var frequency = PI / period

        val below =
            frequency * (power(2, floor(log2(approximateFrequency / frequency)).toInt()) + 1)
        val above = frequency * (power(2, ceil(log2(approximateFrequency / frequency)).toInt()) + 1)

        frequency = if (abs(approximateFrequency - below) < abs(approximateFrequency - above)) {
            below
        } else {
            above
        }

        val amplitude = (if (first.y > second.y) 1 else -1) * deltaY / 2
        return Wave(amplitude, frequency.toFloat(), first.x, verticalShift)
    }

}