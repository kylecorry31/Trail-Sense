package com.kylecorry.trail_sense.shared.colors

import android.graphics.Color
import androidx.annotation.ColorInt
import kotlin.math.max
import kotlin.math.min

object ColorUtils {

    /**
     * Gets the normalized Red to Blue ratio [-1, 1]
     */
    fun nrbr(@ColorInt value: Int): Float {
        val blue = Color.blue(value)
        val red = Color.red(value)
        return (red - blue) / (red + blue).toFloat().coerceAtLeast(1f)
    }

    /**
     * Gets the saturation of the color [0, 1]
     */
    fun saturation(@ColorInt value: Int): Float {
        val blue = Color.blue(value)
        val red = Color.red(value)
        val green = Color.green(value)

        val min = min(red, min(blue, green))
        val max = max(red, max(blue, green))

        if (min == 0 && max == 0) {
            return 0f
        }

        return 1 - min / max.toFloat().coerceAtLeast(1f)
    }

    fun average(@ColorInt value: Int): Float {
        val blue = Color.blue(value)
        val red = Color.red(value)
        val green = Color.green(value)

        return (red + blue + green) / 3f
    }

}