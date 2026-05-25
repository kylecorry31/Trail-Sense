package com.kylecorry.trail_sense.tools.offline_maps.map_layers

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.andromeda_temp.MemoryCachedValue
import com.kylecorry.trail_sense.shared.concurrency.CustomDispatchers
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerParams
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMapFileType
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.mapsforge.MapsforgeTileRenderer
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class TrailMapsTileSource : TileSource {

    @Volatile
    private var rendererHolder: MapsforgeRendererHolder? = null
    private val rendererMutex = Mutex()
    private val service = getAppService<MapService>()
    private val dispatcher = MemoryCachedValue<ExecutorCoroutineDispatcher>(cleanup = {
        it.cancel()
        it.close()
    })

    override suspend fun loadTile(
        context: Context,
        tile: Tile,
        params: Bundle
    ): Bitmap? = onDefault {
        val featureId = params.getString(MapLayerParams.PARAM_FEATURE_ID)?.toLongOrNull()
        val highDetailMode = params.getBoolean(MapLayerParams.PARAM_HIGH_DETAIL_MODE, false)
        val maps = if (featureId == null) {
            service.getAllVectorMaps().filter { it.visible }
        } else {
            listOfNotNull(service.getVectorMap(featureId))
        }.filter { it.type == VectorMapFileType.Mapsforge }
        withContext(getOrCreateDispatcher()) {
            getOrCreateRenderer(maps, highDetailMode).render(context, tile)
        }
    }

    override suspend fun cleanup() {
        clearRenderer()
        dispatcher.reset()
    }

    private suspend fun getOrCreateDispatcher(): ExecutorCoroutineDispatcher {
        return dispatcher.getOrPut { CustomDispatchers.newFixedThreadDispatcher(name = "TrailMapsTileSource") }
    }

    private suspend fun getOrCreateRenderer(
        maps: List<VectorMap>,
        highDetailMode: Boolean
    ): MapsforgeTileRenderer {
        val key = getRendererKey(maps, highDetailMode)
        return rendererMutex.withLock {
            val currentHolder = rendererHolder
            if (currentHolder?.key == key) {
                return@withLock currentHolder.renderer
            }

            currentHolder?.renderer?.clear()
            val renderer = MapsforgeTileRenderer(maps, highDetailMode)
            rendererHolder = MapsforgeRendererHolder(key, renderer)
            renderer
        }
    }

    private suspend fun clearRenderer() = rendererMutex.withLock {
        rendererHolder?.renderer?.clear()
        rendererHolder = null
    }

    private fun getRendererKey(
        maps: List<VectorMap>,
        highDetailMode: Boolean
    ): MapsforgeRendererKey {
        return MapsforgeRendererKey(
            maps.map { it.id },
            highDetailMode
        )
    }

    private data class MapsforgeRendererHolder(
        val key: MapsforgeRendererKey,
        val renderer: MapsforgeTileRenderer
    )

    private data class MapsforgeRendererKey(
        val mapIds: List<Long>,
        val highDetailMode: Boolean
    )

    companion object {
        const val SOURCE_ID = "offline_maps"
    }
}
