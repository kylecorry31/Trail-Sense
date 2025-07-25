package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import androidx.core.graphics.alpha
import androidx.core.graphics.createBitmap
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.ParallelCoroutineRunner
import com.kylecorry.trail_sense.shared.bitmaps.Convert
import com.kylecorry.trail_sense.shared.bitmaps.applyOperations
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
        sourceSelector: ITileSourceSelector,
        bounds: CoordinateBounds,
        metersPerPixel: Float,
        minZoom: Int = 0,
        backgroundColor: Int = Color.WHITE,
        // TODO: This is gross, rather than this it should handle the lifecycle of region loaders and make them distinct
        controlsPdfCache: Boolean = false
    ) = onDefault {
        // Step 1: Split the visible area into tiles (geographic)
        val tiles = TileMath.getTiles(bounds, metersPerPixel.toDouble())
        if (tiles.size > 100) {
            Log.d("TileLoader", "Too many tiles to load: ${tiles.size}")
            return@onDefault
        }

        if ((tiles.firstOrNull()?.z ?: 0) < minZoom) {
            return@onDefault
        }

        // Step 2: For each tile, determine which map(s) will supply it.
        val tileSources = mutableMapOf<Tile, List<IGeographicImageRegionLoader>>()
        for (tile in tiles) {
            val sources = sourceSelector.getRegionLoaders(tile.getBounds())
            if (sources.isNotEmpty()) {
                tileSources[tile] = sources
            }
        }

        // TODO: Handle this cleanup elsewhere
        val allMaps = tileSources.values
            .flatten()
            .filterIsInstance<PhotoMapRegionLoader>()
            .map { it.map }
            .distinct()

        if (controlsPdfCache) {
            PhotoMapRegionLoader.removeUnneededLoaders(allMaps)
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

            val config = if (backgroundColor.alpha != 255) {
                Bitmap.Config.ARGB_8888
            } else {
                Bitmap.Config.RGB_565
            }

            var image =
                createBitmap(source.key.size.width, source.key.size.height, config)
            image.eraseColor(backgroundColor)

            source.value.reversed().forEach {
                val currentImage = it.load(source.key)

                if (currentImage != null) {
                    val canvas = Canvas(image)
                    canvas.drawBitmap(currentImage, 0f, 0f, null)
                    currentImage.recycle()
                }
            }

            // Remove transparency
            image = image.applyOperations(
                Convert(config)
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