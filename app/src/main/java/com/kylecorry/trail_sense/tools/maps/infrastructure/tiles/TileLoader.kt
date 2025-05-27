package com.kylecorry.trail_sense.tools.maps.infrastructure.tiles

import android.graphics.Bitmap
import android.util.Size
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.ParallelCoroutineRunner
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.hypot

class TileLoader {

    var tileCache: Map<Tile, List<Bitmap>> = emptyMap()
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

    suspend fun loadTiles(
        maps: List<PhotoMap>,
        bounds: CoordinateBounds,
        metersPerPixel: Float,
        replaceWhitePixels: Boolean = false
    ) {
        // Step 1: Split the visible area into tiles (geographic)
        val tiles = TileMath.getTiles(bounds, metersPerPixel.toDouble())

        // Step 2: For each tile, determine which map(s) will supply it.
        val tileSources = mutableMapOf<Tile, List<PhotoMap>>()
        val sourceSelector = MercatorTileSourceSelector(maps)
        for (tile in tiles) {
            val sources = sourceSelector.getSources(tile.getBounds())
            if (sources.isNotEmpty()) {
                tileSources[tile] = sources.take(2)
            }
        }

        val newTiles = ConcurrentHashMap<Tile, List<Bitmap>>()
        synchronized(lock) {
            tileCache.keys.forEach { key ->
                if (!tileSources.containsKey(key)) {
                    // TODO: Don't delete the bitmap until the subtiles are loaded
                    tileCache[key]?.forEach { bitmap -> bitmap.recycle() }
                } else {
                    // If the tile is still relevant, keep it
                    newTiles[key] = tileCache[key]!!
                }
            }
            tileCache = newTiles.toMap()
        }

        synchronized(lock) {
            tileCache = newTiles
        }


        val parallel = ParallelCoroutineRunner()

        val middleX = tileSources.keys.map { it.x }.average()
        val middleY = tileSources.keys.map { it.y }.average()

        val sortedEntries = tileSources.entries
            .sortedBy { hypot(it.key.x - middleX, it.key.y - middleY) }

        parallel.run(sortedEntries.toList()) { source ->
            if (newTiles.containsKey(source.key)) {
                return@run
            }
            // Load tiles from the bitmap
            val entries = mutableListOf<Bitmap>()

            synchronized(lock) {
                newTiles[source.key] = entries
            }

            source.value.forEach {
                val loader = PhotoMapRegionLoader(it)
                val image = loader.load(
                    source.key,
                    Size(TileMath.WORLD_TILE_SIZE, TileMath.WORLD_TILE_SIZE),
                    replaceWhitePixels = replaceWhitePixels
                )
                if (image != null) {
                    synchronized(lock) {
                        entries.add(image)
                    }
                }
            }

        }
    }
}