package com.kylecorry.trail_sense.tools.photo_maps.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles.PhotoMapRegionLoader

class PhotoMapLayer : TileMapLayer<PhotoMapTileSource>(
    PhotoMapTileSource(pruneCache = true),
    minZoomLevel = 4
) {

    fun setPreferences(prefs: PhotoMapMapLayerPreferences) {
        percentOpacity = prefs.opacity.get() / 100f
        source.loadPdfs = prefs.loadPdfs.get()
        invalidate()
    }

    override fun setBackgroundColor(color: Int) {
        super.setBackgroundColor(color)
        source.backgroundColor = color
    }

    override fun stop() {
        super.stop()
        PhotoMapRegionLoader.removeUnneededLoaders(emptyList())
    }
}