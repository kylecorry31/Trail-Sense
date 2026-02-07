package com.kylecorry.trail_sense.tools.map.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer

class BaseMapLayer : TileMapLayer<BaseMapTileSource>(BaseMapTileSource()) {
    override val layerId: String = BaseMapTileSource.SOURCE_ID

}
