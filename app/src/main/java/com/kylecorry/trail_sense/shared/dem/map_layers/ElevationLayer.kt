package com.kylecorry.trail_sense.shared.dem.map_layers

import android.os.Bundle
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorMapFactory
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorStrategy
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer
import com.kylecorry.trail_sense.shared.withId

class ElevationLayer : TileMapLayer<ElevationMapTileSource>(
    ElevationMapTileSource(),
    minZoomLevel = 10,
) {

    override val layerId: String = LAYER_ID

    override fun setPreferences(preferences: Bundle) {
        super.setPreferences(preferences)
        val strategyId = preferences.getString(COLOR)?.toLongOrNull()
        source.colorScale = ElevationColorMapFactory().getElevationColorMap(
            ElevationColorStrategy.entries.withId(strategyId ?: 0) ?: DEFAULT_COLOR
        )
        source.highResolution = preferences.getBoolean(HIGH_RESOLUTION, DEFAULT_HIGH_RESOLUTION)
    }

    companion object {
        const val LAYER_ID = "elevation"
        const val COLOR = "color"
        const val HIGH_RESOLUTION = "high_resolution"
        val DEFAULT_COLOR = ElevationColorStrategy.USGS
        const val DEFAULT_HIGH_RESOLUTION = false
    }
}