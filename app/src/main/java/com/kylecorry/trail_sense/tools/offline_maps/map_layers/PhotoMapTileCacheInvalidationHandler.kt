package com.kylecorry.trail_sense.tools.offline_maps.map_layers

import android.os.Bundle
import com.kylecorry.trail_sense.shared.map_layers.tiles.infrastructure.persistance.PersistentTileCache
import com.kylecorry.trail_sense.tools.offline_maps.OfflineMapsToolRegistration
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapType
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class PhotoMapTileCacheInvalidationHandler(private val cache: PersistentTileCache) {

    fun run() {
        Tools.subscribe(OfflineMapsToolRegistration.BROADCAST_OFFLINE_MAP_CHANGED, ::onOfflineMapsChanged)
        Tools.subscribe(OfflineMapsToolRegistration.BROADCAST_OFFLINE_MAP_ADDED, ::onOfflineMapsChanged)
        Tools.subscribe(OfflineMapsToolRegistration.BROADCAST_OFFLINE_MAP_DELETED, ::onOfflineMapsChanged)
    }

    private suspend fun onOfflineMapsChanged(data: Bundle) {
        val mapId = data.getLong(OfflineMapsToolRegistration.BROADCAST_PARAM_OFFLINE_MAP_ID)
        val mapTypeId = data.getLong(OfflineMapsToolRegistration.BROADCAST_PARAM_OFFLINE_MAP_TYPE)

        if (mapTypeId != OfflineMapType.Photo.id) {
            return
        }

        val cacheKeys = listOf(
            "${PhotoMapTileSource.SOURCE_ID}-true-$mapId",
            "${PhotoMapTileSource.SOURCE_ID}-false-$mapId",
            "${PhotoMapTileSource.SOURCE_ID}-null-$mapId",
            "${PhotoMapTileSource.SOURCE_ID}-true",
            "${PhotoMapTileSource.SOURCE_ID}-false",
            "${PhotoMapTileSource.SOURCE_ID}-null",
        )

        cacheKeys.forEach {
            cache.invalidate(it)
        }
    }

}
