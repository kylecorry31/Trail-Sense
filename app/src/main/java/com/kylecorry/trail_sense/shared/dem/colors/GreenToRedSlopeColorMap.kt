package com.kylecorry.trail_sense.shared.dem.colors

import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.ui.colormaps.SampledColorMap
import com.kylecorry.trail_sense.shared.colors.AppColor

class GreenToRedSlopeColorMap : SlopeColorMap {

    private val map = SampledColorMap(
        mapOf(
            5 / 90f to AppColor.Green.color,
            10 / 90f to AppColor.Yellow.color,
            25 / 90f to Colors.interpolate(AppColor.Yellow.color, AppColor.Red.color, 0.5f),
            1f to AppColor.Red.color
        )
    )

    override fun getSlopeColor(degrees: Float): Int {
        return map.getColor(degrees / 90f)
    }
}
