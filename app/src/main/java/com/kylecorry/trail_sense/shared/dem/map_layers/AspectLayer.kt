package com.kylecorry.trail_sense.shared.dem.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer

class AspectLayer : TileMapLayer<AspectMapTileSource>(
    AspectMapTileSource(),
    AspectMapTileSource.SOURCE_ID,
    minZoomLevel = 10
)
