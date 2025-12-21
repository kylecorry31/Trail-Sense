package com.kylecorry.trail_sense.shared.dem.map_layers

import android.os.Bundle
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorMapFactory
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorStrategy
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer
import com.kylecorry.trail_sense.shared.withId

class ElevationLayer(taskRunner: MapLayerBackgroundTask = MapLayerBackgroundTask()) :
    TileMapLayer<ElevationMapTileSource>(
        ElevationMapTileSource(),
        taskRunner,
        minZoomLevel = 10,
    ) {

    override val layerId: String = LAYER_ID

    init {
        preRenderBitmaps = true
    }

    override fun setPreferences(preferences: Bundle) {
        super.setPreferences(preferences)
        val strategyId = preferences.getString(COLOR)?.toLongOrNull()
        source.colorScale = ElevationColorMapFactory().getElevationColorMap(
            ElevationColorStrategy.entries.withId(strategyId ?: 0) ?: ElevationColorStrategy.Brown
        )
    }

    companion object {
        const val LAYER_ID = "elevation"
        const val COLOR = "color"
    }
}