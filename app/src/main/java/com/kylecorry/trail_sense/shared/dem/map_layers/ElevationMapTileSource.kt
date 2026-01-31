package com.kylecorry.trail_sense.shared.dem.map_layers

import android.graphics.Bitmap
import android.os.Bundle
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.trail_sense.shared.andromeda_temp.Dither
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorMap
import com.kylecorry.trail_sense.shared.dem.colors.USGSElevationColorMap
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource

class ElevationMapTileSource : TileSource {

    var colorScale: ElevationColorMap = USGSElevationColorMap()
    var highResolution: Boolean = false

    override suspend fun loadTile(tile: Tile, params: Bundle): Bitmap? {
        val zoomLevel = tile.z.coerceIn(DEM.IMAGE_MIN_ZOOM_LEVEL, DEM.IMAGE_MAX_ZOOM_LEVEL)
        val bounds = tile.getBounds()

        val zoomToResolutionMap = if (highResolution) {
            DEM.HIGH_RESOLUTION_ZOOM_TO_RESOLUTION
        } else {
            DEM.LOW_RESOLUTION_ZOOM_TO_RESOLUTION
        }

        val padding = 2
        return DEM.getElevationImage(
            bounds,
            zoomToResolutionMap[zoomLevel] ?: return null,
            tile.size,
            config = Bitmap.Config.ARGB_8888,
            padding = padding
        ) { x, y, getElevation ->
            colorScale.getElevationColor(getElevation(x, y))
        }.applyOperationsOrNull(
            Dither(Bitmap.Config.RGB_565)
        )
    }
}