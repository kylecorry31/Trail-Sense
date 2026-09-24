package com.kylecorry.trail_sense.tools.offline_maps.map_layers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.cache.InvalidatableCachedValue
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerParams
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.tiles.PhotoMapDecoderCache
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.tiles.PhotoMapTileSourceSelector

class PhotoMapTileSource : TileSource {

    private val backgroundColor: Int = Color.TRANSPARENT
    private val selectorCache = InvalidatableCachedValue<SelectorKey, TileSource>()
    private val decoderCache = PhotoMapDecoderCache()
    private val service = getAppService<OfflineMapService>()

    override suspend fun cleanup() {
        invalidate()
        decoderCache.recycleInactive(emptyList())
    }

    override fun invalidate() {
        selectorCache.invalidate()
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

        val selector = selectorCache.getOrLoad(SelectorKey(featureId, loadPdfs, backgroundColor)) {
            val maps = service.getRenderablePhotoMaps(featureId)
            PhotoMapTileSourceSelector(
                DependencyRegistry.get(),
                maps,
                decoderCache,
                8,
                loadPdfs,
                backgroundColor = backgroundColor
            )
        }
        return selector.loadTile(context, tile, params)
    }

    private data class SelectorKey(val featureId: Long?, val loadPdfs: Boolean, val backgroundColor: Int)

    companion object {
        const val SOURCE_ID = "map"
        const val LOAD_PDFS = "load_pdfs"
        const val DEFAULT_LOAD_PDFS = false
    }
}
