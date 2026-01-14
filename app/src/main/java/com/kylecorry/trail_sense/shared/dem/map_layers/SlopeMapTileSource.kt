package com.kylecorry.trail_sense.shared.dem.map_layers

import android.graphics.Bitmap
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.luna.coroutines.Parallel
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.trail_sense.shared.andromeda_temp.CETL18ColorMap
import com.kylecorry.trail_sense.shared.andromeda_temp.Dither
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.dem.getCellSizeX
import com.kylecorry.trail_sense.shared.dem.getCellSizeY
import com.kylecorry.trail_sense.shared.dem.getSlopeAngle
import com.kylecorry.trail_sense.shared.dem.getSlopeVector
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import kotlin.math.absoluteValue

class SlopeMapTileSource : TileSource {
    var highResolution: Boolean = false
    private val colorMap = CETL18ColorMap()

    override suspend fun load(tiles: List<Tile>, onLoaded: suspend (Tile, Bitmap?) -> Unit) {
        Parallel.forEach(tiles, 16) {
            val bitmap = loadTile(it)
            onLoaded(it, bitmap)
        }
    }

    private suspend fun loadTile(tile: Tile): Bitmap? {
        val zoomLevel = tile.z.coerceIn(DEM.IMAGE_MIN_ZOOM_LEVEL, DEM.IMAGE_MAX_ZOOM_LEVEL)
        val bounds = tile.getBounds()

        val zoomToResolutionMap = if (highResolution) {
            DEM.HIGH_RESOLUTION_ZOOM_TO_RESOLUTION
        } else {
            DEM.LOW_RESOLUTION_ZOOM_TO_RESOLUTION
        }
        val resolution = zoomToResolutionMap[zoomLevel] ?: return null

        val cellSizeX = getCellSizeX(resolution, bounds)
        val cellSizeY = getCellSizeY(resolution)

        val padding = 2
        return DEM.getElevationImage(
            bounds,
            resolution,
            tile.size,
            config = Bitmap.Config.ARGB_8888,
            padding = padding
        ) { x, y, getElevation ->
            val vector = getSlopeVector(cellSizeX, cellSizeY, x, y, getElevation)
            val slopeDegrees = getSlopeAngle(vector).toDegrees().absoluteValue
            // Non-linear scaling to accentuate slopes
            val normalizedSlope = when {
                slopeDegrees <= 20f -> SolMath.map(
                    slopeDegrees,
                    0f,
                    20f,
                    0f,
                    0.5f,
                    shouldClamp = true
                )

                slopeDegrees <= 30f -> SolMath.map(
                    slopeDegrees,
                    20f,
                    30f,
                    0.5f,
                    0.75f,
                    shouldClamp = true
                )

                else -> SolMath.map(slopeDegrees, 30f, 90f, 0.75f, 1f, shouldClamp = true)
            }.coerceIn(0f, 1f)

            colorMap.getColor(normalizedSlope)
        }.applyOperationsOrNull(
            Dither(Bitmap.Config.RGB_565)
        )
    }

}
