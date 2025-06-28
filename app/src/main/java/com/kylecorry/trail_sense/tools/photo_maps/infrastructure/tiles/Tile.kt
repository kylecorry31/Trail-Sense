package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles

import android.util.Size
import com.kylecorry.sol.science.geology.CoordinateBounds
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.sinh

data class Tile(
    val x: Int, val y: Int, val z: Int, val size: Size = Size(
        TileMath.WORLD_TILE_SIZE,
        TileMath.WORLD_TILE_SIZE
    )
) {

    fun getBounds(): CoordinateBounds {
        val n = 1 shl z
        val lonMin = x / n.toDouble() * 360.0 - 180.0
        val lonMax = (x + 1) / n.toDouble() * 360.0 - 180.0

        val latRadMin = atan(sinh(PI * (1 - 2 * (y + 1).toDouble() / n)))
        val latRadMax = atan(sinh(PI * (1 - 2 * y.toDouble() / n)))

        val latMin = Math.toDegrees(latRadMin)
        val latMax = Math.toDegrees(latRadMax)

        return if (lonMax <= 180) {
            CoordinateBounds(latMin, lonMax, latMax, lonMin)
        } else {
            CoordinateBounds(latMin, lonMax - 360, latMax, lonMin - 360)
        }
    }
}