package com.kylecorry.trail_sense.tools.photo_maps.map_layers

import android.graphics.Bitmap
import android.graphics.Color
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileSource
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.MapRepo
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles.PhotoMapTileSourceSelector
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PhotoMapTileSource(
    var backgroundColor: Int = Color.WHITE,
    private val pruneCache: Boolean = false
) : TileSource {

    var loadPdfs = true
    private var lastLoadPdfs = loadPdfs
    private var lastBackgroundColor = backgroundColor
    private var internalSelector: TileSource? = null
    private val lock = Mutex()

    override suspend fun load(tiles: List<Tile>, onLoaded: suspend (Tile, Bitmap?) -> Unit) {
        val selector = lock.withLock {
            if (internalSelector == null || loadPdfs != lastLoadPdfs || backgroundColor != lastBackgroundColor) {
                val repo = AppServiceRegistry.get<MapRepo>()
                internalSelector = PhotoMapTileSourceSelector(
                    AppServiceRegistry.get(),
                    repo.getAllMaps().filter { it.visible },
                    8,
                    loadPdfs,
                    backgroundColor = backgroundColor,
                    pruneCache = pruneCache
                )
                lastLoadPdfs = loadPdfs
                lastBackgroundColor = backgroundColor
            }
            internalSelector
        }
        selector?.load(tiles, onLoaded)
    }
}