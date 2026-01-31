package com.kylecorry.trail_sense.shared.andromeda_temp

import android.graphics.Color
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.core.ui.colormaps.ColorMap

class AlphaColorMap(private val baseColor: Int = Color.BLACK, private val maxAlpha: Int = 255) :
    ColorMap {
    override fun getColor(percent: Float): Int {
        val value = percent.coerceIn(0f, 1f) * 255
        val scale = maxAlpha / 255f
        return baseColor.withAlpha(((255 - value) * scale).toInt())
    }
}