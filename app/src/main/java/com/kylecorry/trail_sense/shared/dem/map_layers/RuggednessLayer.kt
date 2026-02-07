package com.kylecorry.trail_sense.shared.dem.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer

class RuggednessLayer :
    TileMapLayer<RuggednessMapTileSource>(RuggednessMapTileSource(), minZoomLevel = 10) {

    override val layerId: String = LAYER_ID

    companion object {
        const val LAYER_ID = "ruggedness"
        const val HIGH_RESOLUTION = "high_resolution"
        const val DEFAULT_HIGH_RESOLUTION = false
    }
}
