package com.kylecorry.trail_sense.tools.photo_maps.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.TileMapLayer

class PhotoMapLayer : TileMapLayer<PhotoMapTileSource>(PhotoMapTileSource(), minZoomLevel = 4) {
    init {
        controlsPdfCache = true
    }

    fun setPreferences(prefs: PhotoMapMapLayerPreferences) {
        percentOpacity = prefs.opacity.get() / 100f
        source.loadPdfs = prefs.loadPdfs.get()
        invalidate()
    }
}