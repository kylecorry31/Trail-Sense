package com.kylecorry.trail_sense.tools.photo_maps.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap

class PreviewPhotoMapLayer : TileMapLayer<PreviewPhotoMapTileSource>(
    PreviewPhotoMapTileSource(),
    LAYER_ID,
    minZoomLevel = 0
) {

    fun setMap(map: PhotoMap?) {
        source.setMap(map)
        refresh()
    }

    override fun getCacheKey(): String? {
        return null
    }

    companion object {
        const val LAYER_ID = "photo_map_preview"
    }
}
