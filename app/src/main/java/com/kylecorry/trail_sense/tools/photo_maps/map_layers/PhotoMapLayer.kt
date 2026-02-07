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

    override val layerId: String = PhotoMapTileSource.SOURCE_ID
    private var idFilter: Long? = null
    private var loadPdfs: Boolean = PhotoMapTileSource.DEFAULT_LOAD_PDFS
    private val recycleTask = BackgroundTask {
        source.recycle()
    }

    init {
        source.backgroundColor = Color.TRANSPARENT
    }

    override fun setPreferences(preferences: Bundle) {
        super.setPreferences(preferences)
        loadPdfs = preferences.getBoolean(
            PhotoMapTileSource.LOAD_PDFS,
            PhotoMapTileSource.DEFAULT_LOAD_PDFS
        )
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
        keys.add(loadPdfs.toString())
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
        fun getCacheKeysForMap(mapId: Long): List<String> {
            return listOf(
                "${PhotoMapTileSource.SOURCE_ID}-true-$mapId",
                "${PhotoMapTileSource.SOURCE_ID}-false-$mapId",
                "${PhotoMapTileSource.SOURCE_ID}-true",
                "${PhotoMapTileSource.SOURCE_ID}-false",
            )
        }
    }
}
