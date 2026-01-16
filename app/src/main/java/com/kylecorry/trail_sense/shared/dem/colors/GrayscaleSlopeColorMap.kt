package com.kylecorry.trail_sense.shared.dem.colors

import android.graphics.Color
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.ui.colormaps.SampledColorMap

class GrayscaleSlopeColorMap : SlopeColorMap {

    private val map = SampledColorMap(
        mapOf(
            5 / 90f to Color.rgb(20, 20, 20),
            10 / 90f to Color.rgb(127, 127, 127),
            25 / 90f to Colors.interpolate(Color.rgb(127, 127, 127), Color.WHITE, 0.5f),
            1f to Color.WHITE
        )
    )

    override fun getSlopeColor(degrees: Float): Int {
        return map.getColor(degrees / 90f)
    }
}
