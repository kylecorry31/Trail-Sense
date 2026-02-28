package com.kylecorry.trail_sense.tools.photo_maps.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer

class PhotoMapLayer : TileMapLayer<PhotoMapTileSource>(
    PhotoMapTileSource(),
    PhotoMapTileSource.SOURCE_ID,
    minZoomLevel = 4,
    cacheKeys = listOf(
        PhotoMapTileSource.LOAD_PDFS
    )
)
