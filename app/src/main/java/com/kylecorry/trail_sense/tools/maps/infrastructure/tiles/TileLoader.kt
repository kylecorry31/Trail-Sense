package com.kylecorry.trail_sense.tools.maps.infrastructure.tiles

import android.graphics.Bitmap
import android.util.Size
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap

class TileLoader {

    var tileCache: Map<CoordinateBounds, List<Bitmap>> = emptyMap()
        private set

    var lock = Any()

    fun clearCache() {
        synchronized(lock) {
            tileCache.forEach { (_, bitmaps) ->
                bitmaps.forEach { it.recycle() }
            }
            tileCache = emptyMap()
        }
    }

    suspend fun loadTiles(maps: List<PhotoMap>, bounds: CoordinateBounds, viewportSize: Size) {
        // Step 1: Split the visible area into tiles (geographic)
        val tiles = TileMath.getTiles(bounds, viewportSize.width, viewportSize.height)

        // Step 2: For each tile, determine which map(s) will supply it.
        val tileSources = mutableMapOf<CoordinateBounds, List<PhotoMap>>()
        val sourceSelector = MercatorTileSourceSelector(maps)
        for (tile in tiles) {
            val sources = sourceSelector.getSources(tile)
            if (sources.isNotEmpty()) {
                tileSources[tile] = sources.take(2)
            }
        }

        val newTiles = mutableMapOf<CoordinateBounds, List<Bitmap>>()
        synchronized(lock) {
            tileCache.keys.forEach { key ->
                if (!tileSources.any { areEqual(key, it.key) }) {
                    tileCache[key]?.forEach { bitmap -> bitmap.recycle() }
                } else {
                    // If the tile is still relevant, keep it
                    newTiles[key] = tileCache[key]!!
                }
            }

            tileCache = emptyMap()
        }
        for (source in tileSources) {
            if (newTiles.any { areEqual(source.key, it.key) }) {
                continue
            }
            // Load tiles from the bitmap
            val entries = mutableListOf<Bitmap>()
            source.value.forEach {
                val loader = PhotoMapRegionLoader(it)
                loader.load(source.key, Size(256, 256))?.let { entries.add(it) }
            }

            newTiles[source.key] = entries
        }

        synchronized(lock) {
            tileCache = newTiles
        }
    }

    // TODO: Extract to sol
    private fun areEqual(bounds1: CoordinateBounds, bounds2: CoordinateBounds): Boolean {
        return SolMath.isZero((bounds1.north - bounds2.north).toFloat()) &&
                SolMath.isZero((bounds1.west - bounds2.west).toFloat()) &&
                SolMath.isZero((bounds1.south - bounds2.south).toFloat()) &&
                SolMath.isZero((bounds1.east - bounds2.east).toFloat())
    }

}