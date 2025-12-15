package com.kylecorry.trail_sense.tools.photo_maps.map_layers

import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.map_layers.tiles.IGeographicImageRegionLoader
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileSource
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.MapRepo
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles.PhotoMapTileSourceSelector
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PhotoMapTileSource : TileSource {

    var loadPdfs = true
    private var lastLoadPdfs = loadPdfs
    private var internalSelector: TileSource? = null
    private val lock = Mutex()

    override suspend fun getRegionLoaders(bounds: CoordinateBounds): List<IGeographicImageRegionLoader> {
        val selector = lock.withLock {
            if (internalSelector == null || loadPdfs != lastLoadPdfs) {
                val repo = AppServiceRegistry.get<MapRepo>()
                internalSelector = PhotoMapTileSourceSelector(
                    AppServiceRegistry.get(),
                    repo.getAllMaps().filter { it.visible },
                    8,
                    loadPdfs
                )
                lastLoadPdfs = loadPdfs
            }
            internalSelector
        }
        return selector?.getRegionLoaders(bounds) ?: emptyList()
    }
}