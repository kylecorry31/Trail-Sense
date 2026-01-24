package com.kylecorry.trail_sense.shared.dem.map_layers

import android.graphics.Bitmap
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.trail_sense.shared.andromeda_temp.Dither
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.dem.colors.RuggednessColorMap
import com.kylecorry.trail_sense.shared.dem.colors.RuggednessDefaultColorMap
import com.kylecorry.trail_sense.shared.dem.getCellSizeX
import com.kylecorry.trail_sense.shared.dem.getCellSizeY
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import kotlin.math.sqrt

class RuggednessMapTileSource : TileSource {

    var highResolution: Boolean = false
    var colorMap: RuggednessColorMap = RuggednessDefaultColorMap()

    override suspend fun loadTile(tile: Tile): Bitmap? {
        if (tile.z !in DEM.IMAGE_MIN_ZOOM_LEVEL..DEM.IMAGE_MAX_ZOOM_LEVEL) {
            return null
        }
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
        val cellSize = (cellSizeX + cellSizeY) / 2

        val padding = 2
        return DEM.getElevationImage(
            bounds,
            resolution,
            tile.size,
            config = Bitmap.Config.ARGB_8888,
            padding = padding
        ) { x, y, getElevation ->
            val center = getElevation(x, y)
            var sum = 0f

            for (dy in -1..1) {
                for (dx in -1..1) {
                    if (dx == 0 && dy == 0) {
                        continue
                    }
                    val diff = ((getElevation(x + dx, y + dy) - center) / cellSize).toFloat()
                    sum += diff * diff
                }
            }

            val ruggedness = sqrt(sum)
            colorMap.getRuggednessColor(ruggedness)
        }.applyOperationsOrNull(
            Dither(Bitmap.Config.RGB_565)
        )
    }
}
