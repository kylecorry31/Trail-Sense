package com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.map_layers.tiles.IGeographicImageRegionLoader
import com.kylecorry.trail_sense.shared.map_layers.tiles.ITileSourceSelector
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class FullRegionMapTileSource : ITileSourceSelector {

    private var loaderLock = Mutex()
    private var lastLoader: FullRegionMapTileLoader? = null

    abstract fun getLoader(fullBounds: CoordinateBounds): FullRegionMapTileLoader

    override suspend fun getRegionLoaders(bounds: List<CoordinateBounds>): List<List<IGeographicImageRegionLoader>> {
        val fullBounds = CoordinateBounds.from(bounds.flatMap {
            listOf(
                it.northWest,
                it.southEast
            )
        })
        return loaderLock.withLock {
            lastLoader?.close()
            lastLoader = getLoader(fullBounds)
            bounds.map { listOfNotNull(lastLoader) }
        }
    }

    override suspend fun getRegionLoaders(bounds: CoordinateBounds): List<IGeographicImageRegionLoader> {
        return loaderLock.withLock {
            lastLoader?.close()
            lastLoader = getLoader(bounds)
            listOfNotNull(lastLoader)
        }
    }
}