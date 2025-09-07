package com.kylecorry.trail_sense.shared.dem.colors

import android.graphics.Color
import com.kylecorry.andromeda.core.ui.colormaps.SampledColorMap
import com.kylecorry.sol.math.SolMath

/*
<copying>
<authors>
<author href="https://www.usgs.gov/">
<name>U.S. Geological Survey</name>
</author>
<author>
<name>R. Langford</name>
</author>
</authors>
<license>
<informal> Creative Commons Attribution-NonCommercial 3.0 unported </informal>
<year>2021</year>
<text href="https://creativecommons.org/licenses/by-nc/3.0/"/>
</license>
<src>
<format>QGIS file</format>
</src>
<distribute>
<qgis distribute="noncomm" license="ccnc"/>
</distribute>
</copying>
 */
class USGSElevationColorMap : SampledColorMap(
    mapOf(
        0f to Color.rgb(127, 159, 101),
        0.048f to Color.rgb(130, 176, 112),
        0.095f to Color.rgb(160, 194, 124),
        0.143f to Color.rgb(185, 214, 124),
        0.190f to Color.rgb(207, 224, 156),
        0.238f to Color.rgb(223, 233, 168),
        0.286f to Color.rgb(241, 238, 166),
        0.333f to Color.rgb(237, 223, 159),
        0.381f to Color.rgb(240, 210, 141),
        0.429f to Color.rgb(230, 188, 138),
        0.476f to Color.rgb(216, 165, 133),
        0.524f to Color.rgb(197, 149, 135),
        0.571f to Color.rgb(217, 165, 156),
        0.619f to Color.rgb(227, 183, 177),
        0.667f to Color.rgb(223, 192, 191),
        0.714f to Color.rgb(239, 205, 217),
        0.762f to Color.rgb(244, 215, 225),
        0.810f to Color.rgb(243, 223, 233),
        0.857f to Color.rgb(248, 227, 232),
        0.905f to Color.rgb(248, 233, 238),
        0.952f to Color.rgb(246, 240, 245),
        1f to Color.rgb(255, 255, 255)
    )
), ElevationColorMap {
    override fun getElevationColor(meters: Float): Int {
        val low = 0f
        val high = 3048f
        return getColor(SolMath.norm(meters, low, high, shouldClamp = true))
    }
}