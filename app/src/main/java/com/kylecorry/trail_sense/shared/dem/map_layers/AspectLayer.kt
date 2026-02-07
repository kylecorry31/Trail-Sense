package com.kylecorry.trail_sense.shared.dem.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer

class AspectLayer : TileMapLayer<AspectMapTileSource>(AspectMapTileSource(), minZoomLevel = 10) {

    override val layerId: String = AspectMapTileSource.SOURCE_ID
}
