package com.kylecorry.trail_sense.shared.dem.colors

import android.graphics.Color
import com.kylecorry.sol.math.SolMath

// http://seaviewsensing.com/pub/cpt-city/tp/index.html
class TomPattersonElevationColorMap : SampledColorMap(
    mapOf(
        0f / 8000f to Color.rgb(105, 152, 133),
        50f / 8000f to Color.rgb(118, 169, 146),
        200f / 8000f to Color.rgb(131, 181, 155),
        600f / 8000f to Color.rgb(165, 192, 167),
        1000f / 8000f to Color.rgb(211, 201, 179),
        2000f / 8000f to Color.rgb(212, 184, 164),
        3000f / 8000f to Color.rgb(212, 192, 181),
        4000f / 8000f to Color.rgb(214, 209, 206),
        5000f / 8000f to Color.rgb(222, 221, 222),
        6000f / 8000f to Color.rgb(238, 238, 238),
        7000f / 8000f to Color.rgb(246, 247, 246),
        1f to Color.rgb(255, 255, 255)
    )
), ElevationColorMap {
    override fun getElevationColor(meters: Float): Int {
        val min = 0f
        val max = 8000f
        return getColor(SolMath.norm(meters, min, max, shouldClamp = true))
    }
}