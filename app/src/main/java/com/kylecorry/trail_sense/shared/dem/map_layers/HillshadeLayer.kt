package com.kylecorry.trail_sense.shared.dem.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer

class HillshadeLayer :
    TileMapLayer<HillshadeMapTileSource>(HillshadeMapTileSource(), minZoomLevel = 10) {

    override val layerId: String = HillshadeMapTileSource.SOURCE_ID

    override val isTimeDependent: Boolean = true

    init {
        shouldMultiply = true
    }
}
