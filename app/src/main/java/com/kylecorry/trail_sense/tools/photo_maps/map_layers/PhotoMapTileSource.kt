package com.kylecorry.trail_sense.tools.photo_maps.map_layers

import android.graphics.Bitmap
import android.graphics.Color
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.MapRepo
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles.PhotoMapTileSourceSelector
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PhotoMapTileSource(
    var backgroundColor: Int = Color.WHITE,
    private val pruneCache: Boolean = false,
) : TileSource {

    var filter: (map: PhotoMap) -> Boolean = { it.visible }
    var loadPdfs = true
    private var lastLoadPdfs = loadPdfs
    private var lastBackgroundColor = backgroundColor
    private var lastFilter = filter
    private var internalSelector: TileSource? = null
    private val lock = Mutex()

    override suspend fun load(tiles: List<Tile>, onLoaded: (Tile, Bitmap?) -> Unit) {
        val selector = lock.withLock {
            if (internalSelector == null || loadPdfs != lastLoadPdfs || backgroundColor != lastBackgroundColor || filter != lastFilter) {
                val repo = AppServiceRegistry.get<MapRepo>()
                internalSelector = PhotoMapTileSourceSelector(
                    AppServiceRegistry.get(),
                    repo.getAllMaps().filter(filter),
                    8,
                    loadPdfs,
                    backgroundColor = backgroundColor,
                    pruneCache = pruneCache
                )
                lastLoadPdfs = loadPdfs
                lastBackgroundColor = backgroundColor
                lastFilter = filter
            }
            internalSelector
        }
        selector?.load(tiles, onLoaded)
    }
}