package com.kylecorry.trail_sense.tools.tides.domain

import kotlin.math.cos

data class SineWave(
    val amplitude: Float,
    val frequency: Float,
    val horizontalShift: Float,
    val verticalShift: Float
) {
    fun cosine(x: Float): Float {
        return amplitude * cos(frequency * (x - horizontalShift)) + verticalShift
    }

    fun sine(x: Float): Float {
        return amplitude * cos(frequency * (x - horizontalShift)) + verticalShift
    }
}
