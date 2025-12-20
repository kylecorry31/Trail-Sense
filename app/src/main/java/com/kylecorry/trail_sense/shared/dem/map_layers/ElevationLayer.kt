package com.kylecorry.trail_sense.shared.dem.map_layers

import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorMapFactory
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer

class ElevationLayer(taskRunner: MapLayerBackgroundTask = MapLayerBackgroundTask()) :
    TileMapLayer<ElevationMapTileSource>(
        ElevationMapTileSource(),
        taskRunner,
        minZoomLevel = 10,
    ) {

    init {
        preRenderBitmaps = true
    }

    fun setPreferences(prefs: ElevationMapLayerPreferences) {
        percentOpacity = prefs.opacity.get() / 100f
        source.colorScale =
            ElevationColorMapFactory().getElevationColorMap(prefs.colorStrategy.get())
        invalidate()
    }
}