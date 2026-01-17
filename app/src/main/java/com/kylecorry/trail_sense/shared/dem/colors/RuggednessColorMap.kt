package com.kylecorry.trail_sense.shared.dem.colors

import androidx.annotation.ColorInt
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.ui.colormaps.SampledColorMap
import com.kylecorry.trail_sense.shared.colors.AppColor

interface RuggednessColorMap {
    @ColorInt
    fun getRuggednessColor(ruggedness: Float): Int
}

class RuggednessDefaultColorMap : RuggednessColorMap {

    // https://www.researchgate.net/figure/Terrain-Ruggedness-Index-TRI-Categories-from-Riley-et-al-102_tbl1_272661635
    private val CATEGORY_RESOLUTION = 1000f
    private val CATEGORY_LEVEL = 80 / CATEGORY_RESOLUTION
    private val CATEGORY_NEARLY_LEVEL = 116 / CATEGORY_RESOLUTION
    private val CATEGORY_INTERMEDIATELY_RUGGED = 239 / CATEGORY_RESOLUTION
    private val CATEGORY_MODERATELY_RUGGED = 497 / CATEGORY_RESOLUTION
    private val CATEGORY_HIGHLY_RUGGED = 958 / CATEGORY_RESOLUTION
    private val CATEGORY_EXTREMELY_RUGGED = 4367 / CATEGORY_RESOLUTION

    private val MAX = CATEGORY_EXTREMELY_RUGGED

    private val map = SampledColorMap(
        mapOf(
            0f to AppColor.Green.color,
            CATEGORY_LEVEL / MAX to Colors.interpolate(
                AppColor.Green.color,
                AppColor.Yellow.color,
                0.5f
            ),
            CATEGORY_NEARLY_LEVEL / MAX to AppColor.Yellow.color,
            CATEGORY_INTERMEDIATELY_RUGGED / MAX to Colors.interpolate(
                AppColor.Yellow.color,
                AppColor.Orange.color,
                0.5f
            ),
            CATEGORY_MODERATELY_RUGGED / MAX to AppColor.Orange.color,
            CATEGORY_HIGHLY_RUGGED / MAX to Colors.interpolate(
                AppColor.Orange.color,
                AppColor.Red.color,
                0.5f
            ),
            1f to AppColor.Red.color
        )
    )

    override fun getRuggednessColor(ruggedness: Float): Int {
        val normalized = (ruggedness / MAX).coerceIn(0f, 1f)
        return map.getColor(normalized)
    }
}
