package com.kylecorry.trail_sense.tools.photo_maps.map_layers

import android.content.Context

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerParams
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.MapRepo
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles.PhotoMapDecoderCache
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles.PhotoMapTileSourceSelector
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PhotoMapTileSource : TileSource {

    private var lastLoadPdfs = DEFAULT_LOAD_PDFS
    private val backgroundColor: Int = Color.TRANSPARENT
    private var lastBackgroundColor = backgroundColor
    private var lastFeatureId: Long? = null
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
        val featureId = params.getString(MapLayerParams.PARAM_FEATURE_ID)?.toLongOrNull()

        val selector = lock.withLock {
            if (internalSelector == null || loadPdfs != lastLoadPdfs || backgroundColor != lastBackgroundColor || featureId != lastFeatureId) {
                val repo = AppServiceRegistry.get<MapRepo>()
                val maps = if (featureId == null) {
                    repo.getAllMaps().filter { it.visible }
                } else {
                    repo.getAllMaps().filter { it.id == featureId }
                }
                internalSelector = PhotoMapTileSourceSelector(
                    AppServiceRegistry.get(),
                    maps,
                    decoderCache,
                    8,
                    loadPdfs,
                    backgroundColor = backgroundColor
                )
                lastLoadPdfs = loadPdfs
                lastBackgroundColor = backgroundColor
                lastFeatureId = featureId
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
