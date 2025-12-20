package com.kylecorry.trail_sense.tools.photo_maps.map_layers

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles.PhotoMapRegionLoader

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

    fun improveResolution(
        bounds: CoordinateBounds,
        metersPerPixel: Double,
        minimumTileCount: Int
    ) {
        val zoom = TileMath.getZoomLevel(bounds, metersPerPixel)
        var tileCount: Int
        var zoomOffset = -1
        do {
            zoomOffset++
            tileCount = TileMath.getTiles(bounds, zoom + zoomOffset).size
        } while (tileCount < minimumTileCount && zoomOffset < 10)

        setZoomOffset(zoomOffset)
        notifyListeners()
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