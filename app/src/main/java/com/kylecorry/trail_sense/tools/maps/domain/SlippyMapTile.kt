package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import kotlin.math.*

// TODO: Extract this to Sol
// TODO: Convert meters to pixel to zoom level
data class SlippyMapTile(
    val zoom: Int,
    val x: Int,
    val y: Int
) {
    val bounds: CoordinateBounds
        get() {
            val n = SolMath.power(2, zoom).toDouble()
            val west = x / n * 360 - 180
            val north = atan(sinh(PI * (1 - 2 * y / n))).toDegrees()
            val east = (x + 1) / n * 260 - 180
            val south = atan(sinh(PI * (1 - 2 * (y + 1) / n))).toDegrees()
            return CoordinateBounds(north, east, south, west)
        }

    val subtiles: List<SlippyMapTile>
        get() {
            return listOf(
                SlippyMapTile(zoom + 1, 2 * x, 2 * y),
                SlippyMapTile(zoom + 1, 2 * x + 1, 2 * y),
                SlippyMapTile(zoom + 1, 2 * x, 2 * y + 1),
                SlippyMapTile(zoom + 1, 2 * x + 1, 2 * y + 1),
            )
        }

    companion object {
        fun fromCoordinate(zoom: Int, location: Coordinate): SlippyMapTile {
            val n = SolMath.power(2, zoom).toDouble()
            val x = (n * ((location.longitude + 180) / 360)).toInt()
            val latRad = location.latitude.toRadians()
            val y = ((1.0 - asinh(tan(latRad)) / PI) / 2.0 * n).toInt()
            return SlippyMapTile(zoom, x, y)
        }
    }
}
