package com.kylecorry.trail_sense.shared.dem.map_layers

import android.content.Context

import android.graphics.Bitmap
import android.os.Bundle
import com.kylecorry.andromeda.bitmaps.operations.Dither
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.dem.colors.RuggednessDefaultColorMap
import com.kylecorry.trail_sense.shared.dem.getCellSizeX
import com.kylecorry.trail_sense.shared.dem.getCellSizeY
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import kotlin.math.sqrt

class RuggednessMapTileSource : TileSource {

    override suspend fun loadTile(context: Context, tile: Tile, params: Bundle): Bitmap? {
        val preferences = params.getBundle(TileSource.PARAM_PREFERENCES)
        val highResolution =
            preferences?.getBoolean(
                HIGH_RESOLUTION,
                DEFAULT_HIGH_RESOLUTION
            ) ?: DEFAULT_HIGH_RESOLUTION
        val colorMap = RuggednessDefaultColorMap()

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

    companion object {
        const val SOURCE_ID = "ruggedness"
        const val HIGH_RESOLUTION = "high_resolution"
        const val DEFAULT_HIGH_RESOLUTION = false
    }
}
