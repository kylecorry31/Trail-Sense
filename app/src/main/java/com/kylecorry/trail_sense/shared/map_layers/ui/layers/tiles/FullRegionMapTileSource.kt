package com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles

import android.graphics.Bitmap
import com.kylecorry.luna.coroutines.Parallel
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class FullRegionMapTileSource : TileSource {

    private var loaderLock = Mutex()
    private var lastLoader: FullRegionMapTileLoader? = null

    abstract fun getLoader(fullBounds: CoordinateBounds): FullRegionMapTileLoader

    override suspend fun load(tiles: List<Tile>, onLoaded: (Tile, Bitmap?) -> Unit) {
        val fullBounds = CoordinateBounds.from(tiles.flatMap {
            val bounds = it.getBounds()
            listOf(
                bounds.northWest,
                bounds.southEast
            )
        }, checkForFullWorld = false)
        loaderLock.withLock {
            lastLoader?.close()
            lastLoader = getLoader(fullBounds)
            Parallel.forEach(tiles) {
                onLoaded(it, lastLoader?.load(it))
            }
        }
    }
}