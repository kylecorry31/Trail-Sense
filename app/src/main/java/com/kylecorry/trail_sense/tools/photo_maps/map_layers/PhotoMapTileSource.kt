package com.kylecorry.trail_sense.tools.photo_maps.map_layers

import android.content.Context

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.MapRepo
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles.PhotoMapDecoderCache
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles.PhotoMapTileSourceSelector
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PhotoMapTileSource : TileSource {

    var filter: (map: PhotoMap) -> Boolean = { it.visible }
    private var lastLoadPdfs = DEFAULT_LOAD_PDFS
    private val backgroundColor: Int = Color.TRANSPARENT
    private var lastBackgroundColor = backgroundColor
    private var lastFilter = filter
    private var internalSelector: TileSource? = null
    private val lock = Mutex()
    private val decoderCache = PhotoMapDecoderCache()

    override suspend fun cleanup() {
        decoderCache.recycleInactive(emptyList())
    }

    override suspend fun loadTile(
        context: Context,
        tile: Tile,
        params: Bundle
    ): Bitmap? {
        val preferences = params.getPreferences()
        val loadPdfs = preferences.getBoolean(
            LOAD_PDFS,
            DEFAULT_LOAD_PDFS
        )

        val selector = lock.withLock {
            if (internalSelector == null || loadPdfs != lastLoadPdfs || backgroundColor != lastBackgroundColor || filter != lastFilter) {
                val repo = AppServiceRegistry.get<MapRepo>()
                internalSelector = PhotoMapTileSourceSelector(
                    AppServiceRegistry.get(),
                    repo.getAllMaps().filter(filter),
                    decoderCache,
                    8,
                    loadPdfs,
                    backgroundColor = backgroundColor
                )
                lastLoadPdfs = loadPdfs
                lastBackgroundColor = backgroundColor
                lastFilter = filter
            }
            internalSelector
        }
        return selector?.loadTile(context, tile, params)
    }

    companion object {
        const val SOURCE_ID = "map"
        const val LOAD_PDFS = "load_pdfs"
        const val DEFAULT_LOAD_PDFS = false
    }
}
