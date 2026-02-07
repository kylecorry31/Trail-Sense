package com.kylecorry.trail_sense.shared.dem.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorStrategy

class ElevationLayer : TileMapLayer<ElevationMapTileSource>(
    ElevationMapTileSource(),
    minZoomLevel = 10,
) {

    override val layerId: String = LAYER_ID

    companion object {
        const val LAYER_ID = "elevation"
        const val COLOR = "color"
        const val HIGH_RESOLUTION = "high_resolution"
        val DEFAULT_COLOR = ElevationColorStrategy.USGS
        const val DEFAULT_HIGH_RESOLUTION = false
    }
}
