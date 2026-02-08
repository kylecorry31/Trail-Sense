package com.kylecorry.trail_sense.shared.dem.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer

class ElevationLayer : TileMapLayer<ElevationMapTileSource>(
    ElevationMapTileSource(),
    ElevationMapTileSource.SOURCE_ID,
    minZoomLevel = 10
)
