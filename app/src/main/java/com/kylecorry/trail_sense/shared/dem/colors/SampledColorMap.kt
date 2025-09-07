package com.kylecorry.trail_sense.shared.dem.colors

import android.graphics.Color
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.ui.colormaps.ColorMap
import com.kylecorry.andromeda.core.ui.colormaps.RgbInterpolationColorMap

open class SampledColorMap(
    samples: Map<Float, Int>,
    interpolationResolution: Float = 0.05f
) : ColorMap {
    private val interpolator =
        RgbInterpolationColorMap(getNewColorMap(samples, interpolationResolution))

    override fun getColor(percent: Float): Int {
        return interpolator.getColor(percent)
    }

    private fun getNewColorMap(
        samples: Map<Float, Int>,
        interpolationResolution: Float
    ): Array<Int> {
        val colorMap = mutableListOf<Int>()
        var current = 0f
        val entries = samples.entries.sortedBy { it.key }
        while (current <= 1f) {
            colorMap.add(getInterpolatedColor(current, entries))
            current += interpolationResolution
        }
        return colorMap.toTypedArray()
    }

    private fun getInterpolatedColor(percent: Float, samples: List<Map.Entry<Float, Int>>): Int {
        if (samples.isEmpty()) {
            return Color.BLACK
        }
        if (samples.size == 1) {
            return samples[0].value
        }

        val clampedPercent = percent.coerceIn(0f, 1f)

        val lower = samples.lastOrNull { it.key <= clampedPercent } ?: samples.first()
        val upper = samples.firstOrNull { it.key >= clampedPercent } ?: samples.last()

        if (lower == upper) {
            return lower.value
        }

        val range = upper.key - lower.key
        val relativePercent = if (range == 0f) 0f else (clampedPercent - lower.key) / range

        return Colors.interpolate(lower.value, upper.value, relativePercent)
    }
}