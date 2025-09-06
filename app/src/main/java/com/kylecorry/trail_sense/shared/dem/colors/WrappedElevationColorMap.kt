package com.kylecorry.trail_sense.shared.dem.colors

import com.kylecorry.andromeda.core.ui.colormaps.ColorMap
import com.kylecorry.sol.math.SolMath

class WrappedElevationColorMap(
    private val map: ColorMap,
    private val minElevation: Float,
    private val maxElevation: Float
) : ElevationColorMap {
    override fun getElevationColor(meters: Float): Int {
        return getColor(SolMath.norm(meters, minElevation, maxElevation, shouldClamp = true))
    }

    override fun getColor(percent: Float): Int {
        return map.getColor(percent)
    }
}