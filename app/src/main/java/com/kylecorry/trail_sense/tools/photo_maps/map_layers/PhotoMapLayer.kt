package com.kylecorry.trail_sense.tools.photo_maps.map_layers

import android.graphics.Color
import android.os.Bundle
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.andromeda_temp.BackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap

class PhotoMapLayer : TileMapLayer<PhotoMapTileSource>(
    PhotoMapTileSource(),
    minZoomLevel = 4
) {

    override val layerId: String = LAYER_ID
    private val recycleTask = BackgroundTask {
        source.recycle()
    }

    init {
        source.backgroundColor = Color.TRANSPARENT
    }

    override fun setPreferences(preferences: Bundle) {
        super.setPreferences(preferences)
        source.loadPdfs = preferences.getBoolean(LOAD_PDFS, DEFAULT_LOAD_PDFS)
    }

    fun setPhotoMapFilter(filter: (map: PhotoMap) -> Boolean) {
        source.filter = filter
    }

    fun improveResolution(
        bounds: CoordinateBounds,
        metersPerPixel: Float,
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

    override fun stop() {
        super.stop()
        recycleTask.start()
    }

    companion object {
        const val LAYER_ID = "map"
        const val LOAD_PDFS = "load_pdfs"
        const val DEFAULT_LOAD_PDFS = false
    }
}