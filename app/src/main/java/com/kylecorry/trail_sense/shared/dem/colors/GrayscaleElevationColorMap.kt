package com.kylecorry.trail_sense.shared.dem.colors

import android.graphics.Color
import com.kylecorry.andromeda.core.ui.colormaps.RgbInterpolationColorMap
import com.kylecorry.sol.math.SolMath

class GrayscaleElevationColorMap : RgbInterpolationColorMap(
    arrayOf(
        Color.rgb(75, 75, 75),
        Color.rgb(250, 250, 250),
    )
), ElevationColorMap {
    override fun getElevationColor(meters: Float): Int {
        val min = 0f
        val max = 3000f
        return getColor(SolMath.norm(meters, min, max, shouldClamp = true))
    }
}