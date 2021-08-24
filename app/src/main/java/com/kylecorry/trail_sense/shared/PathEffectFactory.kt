package com.kylecorry.trail_sense.shared

import android.graphics.PathEffect
import com.kylecorry.andromeda.canvas.ArrowPathEffect
import kotlin.math.floor

class PathEffectFactory {

    fun getArrowPathEffect(pathLength: Float, scale: Float = 1f): PathEffect {
        val minArrowSpacing = 50 / scale
        val n = floor(pathLength / minArrowSpacing)
        val r = pathLength - n * minArrowSpacing
        return ArrowPathEffect(
            6f / scale,
            minArrowSpacing + if (n != 0f) r / n else 0f
        )
    }

}