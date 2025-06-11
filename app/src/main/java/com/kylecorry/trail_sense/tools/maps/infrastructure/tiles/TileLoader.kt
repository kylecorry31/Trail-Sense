package com.kylecorry.trail_sense.tools.maps.infrastructure.tiles

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import androidx.core.graphics.createBitmap
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.ParallelCoroutineRunner
import com.kylecorry.trail_sense.shared.bitmaps.Convert
import com.kylecorry.trail_sense.shared.bitmaps.applyOperations
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
        replaceWhitePixels: Boolean = false,
        minZoom: Int = 0,
        backgroundColor: Int = Color.WHITE
    ) {
        // Step 1: Split the visible area into tiles (geographic)
        val tiles = TileMath.getTiles(bounds, metersPerPixel.toDouble())
        if (tiles.size > 100) {
            Log.d("TileLoader", "Too many tiles to load: ${tiles.size}")
            return
        }

        if ((tiles.firstOrNull()?.z ?: 0) < minZoom) {
            return
        }

        // Step 2: For each tile, determine which map(s) will supply it.
        val tileSources = mutableMapOf<Tile, List<PhotoMap>>()
        val sourceSelector = PhotoMapTileSourceSelector(maps, 4)
        for (tile in tiles) {
            val sources = sourceSelector.getSources(tile.getBounds())
            if (sources.isNotEmpty()) {
                tileSources[tile] = sources
            }
        }

        var hasChanges = false

        val newTiles = ConcurrentHashMap<Tile, List<Bitmap>>()
        synchronized(lock) {
            tileCache.keys.forEach { key ->
                newTiles[key] = tileCache[key] ?: return@forEach
            }
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

            var image =
                createBitmap(source.key.size.width, source.key.size.height, Bitmap.Config.RGB_565)
            image.eraseColor(backgroundColor)

            source.value.reversed().forEach {
                val loader = PhotoMapRegionLoader(it)
                val currentImage = loader.load(
                    source.key,
                    replaceWhitePixels = replaceWhitePixels
                )

                if (currentImage != null) {
                    val canvas = Canvas(image)
                    canvas.drawBitmap(currentImage, 0f, 0f, null)
                    currentImage.recycle()
                }
            }

            // Remove transparency
            image = image.applyOperations(
                Convert(Bitmap.Config.RGB_565)
            )

            hasChanges = true
            synchronized(lock) {
                entries.add(image)
            }
        }

        synchronized(lock) {
            val toDelete = mutableListOf<Tile>()
            tileCache.keys.forEach { key ->
                if (!tileSources.containsKey(key)) {
                    tileCache[key]?.forEach { bitmap -> bitmap.recycle() }
                    toDelete.add(key)
                    hasChanges = true
                }
            }

            tileCache = tileCache.filterKeys { it in tileSources.keys }
        }

        if (hasChanges) {
            System.gc()
            val memoryUsage = tileCache.values.sumOf { bitmaps ->
                bitmaps.sumOf { it.allocationByteCount }
            }
            Log.d("TileLoader", "Tile memory usage: ${memoryUsage / 1024} KB (${tiles.size} tiles)")
        }
    }
}