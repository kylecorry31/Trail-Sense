package com.kylecorry.trail_sense.tools.photo_maps.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer

class PhotoMapLayer(private val photoMapId: Long? = null) : TileMapLayer<PhotoMapTileSource>(
    PhotoMapTileSource(pruneCache = true) {
        if (photoMapId == null) {
            it.visible
        } else {
            it.id == photoMapId
        }
    },
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
}