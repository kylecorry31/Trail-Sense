package com.kylecorry.trail_sense.shared.dem.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer

class RuggednessLayer :
    TileMapLayer<RuggednessMapTileSource>(RuggednessMapTileSource(), minZoomLevel = 10) {

    override val layerId: String = RuggednessMapTileSource.SOURCE_ID
}
