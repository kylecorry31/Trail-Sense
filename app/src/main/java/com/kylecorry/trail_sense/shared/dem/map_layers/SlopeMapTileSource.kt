package com.kylecorry.trail_sense.shared.dem.map_layers

import android.graphics.Bitmap
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.luna.coroutines.Parallel
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.trail_sense.shared.andromeda_temp.Dither
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.dem.colors.GreenToRedSlopeColorMap
import com.kylecorry.trail_sense.shared.dem.colors.SlopeColorMap
import com.kylecorry.trail_sense.shared.dem.getCellSizeX
import com.kylecorry.trail_sense.shared.dem.getCellSizeY
import com.kylecorry.trail_sense.shared.dem.getSlopeAngle
import com.kylecorry.trail_sense.shared.dem.getSlopeVector
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import kotlin.math.absoluteValue

class SlopeMapTileSource : TileSource {
    var highResolution: Boolean = false
    var colorMap: SlopeColorMap = GreenToRedSlopeColorMap()
    var smooth = true

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

            val actualDegrees = if (smooth) {
                slopeDegrees
            } else {
                when {
                    slopeDegrees <= 10f -> 0f
                    slopeDegrees <= 25f -> 10f
                    else -> 90f
                }
            }

            colorMap.getSlopeColor(actualDegrees)
        }.applyOperationsOrNull(
            Dither(Bitmap.Config.RGB_565)
        )
    }

}
