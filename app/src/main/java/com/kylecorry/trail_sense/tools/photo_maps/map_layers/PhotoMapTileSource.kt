package com.kylecorry.trail_sense.tools.photo_maps.map_layers

import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.map_layers.tiles.IGeographicImageRegionLoader
import com.kylecorry.trail_sense.shared.map_layers.tiles.ITileSourceSelector
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.MapRepo
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles.PhotoMapTileSourceSelector

class PhotoMapTileSource : ITileSourceSelector {

    var loadPdfs = true
    private var lastLoadPdfs = loadPdfs
    private var internalSelector: ITileSourceSelector? = null

    override suspend fun getRegionLoaders(bounds: CoordinateBounds): List<IGeographicImageRegionLoader> {
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
        return internalSelector?.getRegionLoaders(bounds) ?: emptyList()
    }
}