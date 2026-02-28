package com.kylecorry.trail_sense.tools.photo_maps.map_layers

import android.os.Bundle
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer

class PhotoMapLayer : TileMapLayer<PhotoMapTileSource>(
    PhotoMapTileSource(),
    PhotoMapTileSource.SOURCE_ID,
    minZoomLevel = 4
) {
    private var loadPdfs: Boolean = PhotoMapTileSource.DEFAULT_LOAD_PDFS

    override fun setPreferences(preferences: Bundle) {
        super.setPreferences(preferences)
        loadPdfs = preferences.getBoolean(
            PhotoMapTileSource.LOAD_PDFS,
            PhotoMapTileSource.DEFAULT_LOAD_PDFS
        )
    }

    override fun getBaseCacheKey(): String {
        val keys = mutableListOf(layerId)
        keys.add(loadPdfs.toString())
        return keys.joinToString("-")
    }
}
