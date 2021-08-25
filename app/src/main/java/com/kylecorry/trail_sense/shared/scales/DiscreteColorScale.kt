package com.kylecorry.trail_sense.shared.scales

import android.graphics.Color
import androidx.annotation.ColorInt

class DiscreteColorScale(@ColorInt private val colors: List<Int>) :
    IColorScale {
    override fun getColor(percent: Float): Int {
        if (colors.isEmpty()) {
            return Color.BLACK
        }

        val percentPerColor = 1 / colors.size.toFloat()

        for (i in colors.indices) {
            if (percent <= percentPerColor) {
                return colors[i]
            }
        }

        return colors.last()
    }
}