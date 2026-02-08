package com.kylecorry.trail_sense.shared.dem.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer

class SlopeLayer : TileMapLayer<SlopeMapTileSource>(
    SlopeMapTileSource(),
    SlopeMapTileSource.SOURCE_ID,
    minZoomLevel = 10
)
