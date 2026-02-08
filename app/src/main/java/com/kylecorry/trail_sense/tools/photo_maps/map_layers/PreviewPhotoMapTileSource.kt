package com.kylecorry.trail_sense.tools.photo_maps.map_layers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles.PhotoMapDecoderCache
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles.PhotoMapTileSourceSelector
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PreviewPhotoMapTileSource : TileSource {

    private val decoderCache = PhotoMapDecoderCache()
    private val lock = Mutex()

    private var map: PhotoMap? = null
    private var lastMapHash: Int? = null
    private var lastLoadPdfs = PhotoMapTileSource.DEFAULT_LOAD_PDFS
    private var internalSelector: PhotoMapTileSourceSelector? = null

    fun setMap(map: PhotoMap?) {
        this.map = map
    }

    override suspend fun loadTile(
        context: Context,
        tile: Tile,
        params: Bundle
    ): Bitmap? {
        val map = map ?: return null
        val preferences = params.getPreferences()
        val loadPdfs = preferences.getBoolean(
            PhotoMapTileSource.LOAD_PDFS,
            PhotoMapTileSource.DEFAULT_LOAD_PDFS
        )
        val mapHash = map.hashCode()

        val selector = lock.withLock {
            if (internalSelector == null || mapHash != lastMapHash || loadPdfs != lastLoadPdfs) {
                internalSelector = PhotoMapTileSourceSelector(
                    context,
                    listOf(map),
                    decoderCache,
                    maxLayers = 1,
                    loadPdfs = loadPdfs,
                    backgroundColor = Color.TRANSPARENT
                )
                lastMapHash = mapHash
                lastLoadPdfs = loadPdfs
            }
            internalSelector
        }

        return selector?.loadTile(context, tile, params)
    }

    override suspend fun cleanup() {
        decoderCache.recycleInactive(emptyList())
    }
}
