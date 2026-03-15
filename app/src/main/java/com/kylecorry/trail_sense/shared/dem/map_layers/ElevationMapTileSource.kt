package com.kylecorry.trail_sense.shared.dem.map_layers

import android.content.Context

import android.graphics.Bitmap
import android.os.Bundle
import com.kylecorry.andromeda.bitmaps.operations.Dither
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorMapFactory
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorStrategy
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.map.map_layers.BaseMapTileSource

class ElevationMapTileSource : TileSource {

    override suspend fun loadTile(context: Context, tile: Tile, params: Bundle): Bitmap? {
        val preferences = params.getPreferences()
        val strategyId = preferences.getString(COLOR)?.toLongOrNull()
        val colorScale = ElevationColorMapFactory().getElevationColorMap(
            ElevationColorStrategy.entries.withId(strategyId ?: 0) ?: DEFAULT_COLOR
        )
        val highResolution =
            preferences.getBoolean(
                HIGH_RESOLUTION,
                DEFAULT_HIGH_RESOLUTION
            )

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
            padding = padding,
            oceanColor = OCEAN_COLOR,
            inlandWaterColor = INLAND_WATER_COLOR
        ) { x, y, getElevation ->
            colorScale.getElevationColor(getElevation(x, y))
        }.applyOperationsOrNull(
            Dither(Bitmap.Config.RGB_565)
        )
    }

    companion object {
        private val OCEAN_COLOR = BaseMapTileSource.DESTINATION_MAP_COLOR_OCEAN
        private val INLAND_WATER_COLOR = BaseMapTileSource.DESTINATION_MAP_COLOR_INLAND_WATER
        const val SOURCE_ID = "elevation"
        const val COLOR = "color"
        const val HIGH_RESOLUTION = "high_resolution"
        val DEFAULT_COLOR = ElevationColorStrategy.USGS
        const val DEFAULT_HIGH_RESOLUTION = false
    }
}
