package com.kylecorry.trail_sense.tools.photo_maps.map_layers

import android.graphics.Color
import android.os.Bundle
import com.kylecorry.luna.coroutines.BackgroundTask
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer

class PhotoMapLayer : TileMapLayer<PhotoMapTileSource>(
    PhotoMapTileSource(),
    minZoomLevel = 4
) {

    override val layerId: String = LAYER_ID
    private var idFilter: Long? = null
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

    fun setPhotoMapFilter(id: Long? = null) {
        idFilter = id
        source.filter = if (id == null) {
            { true }
        } else {
            { it.id == id }
        }
    }

    override fun getCacheKey(): String {
        val keys = mutableListOf(layerId)
        keys.add(source.loadPdfs.toString())
        idFilter?.let { keys.add(it.toString()) }
        return keys.joinToString("-")
    }

    fun improveResolution(
        bounds: CoordinateBounds,
        zoom: Int,
        minimumTileCount: Int
    ) {
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

        fun getCacheKeysForMap(mapId: Long): List<String> {
            return listOf(
                "$LAYER_ID-true-$mapId",
                "$LAYER_ID-false-$mapId",
                "$LAYER_ID-true",
                "$LAYER_ID-false",
            )
        }

    }
}