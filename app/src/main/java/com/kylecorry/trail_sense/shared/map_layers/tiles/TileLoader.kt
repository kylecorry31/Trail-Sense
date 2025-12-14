package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import androidx.core.graphics.alpha
import androidx.core.graphics.createBitmap
import com.kylecorry.andromeda.bitmaps.operations.Conditional
import com.kylecorry.andromeda.bitmaps.operations.Convert
import com.kylecorry.andromeda.bitmaps.operations.ReplaceColor
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.luna.coroutines.ParallelCoroutineRunner
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles.PhotoMapRegionLoader
import kotlin.math.hypot

class TileLoader {

    var tileCache: Map<Tile, List<Bitmap>> = emptyMap()
        private set

    var lock = Any()

    var useFirstImageSize: Boolean = false

    var alwaysReloadTiles: Boolean = false
    var clearTileWhenNullResponse: Boolean = true

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
        controlsPdfCache: Boolean = false,
        onChange: suspend () -> Unit = {}
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
        val loaders = sourceSelector.getRegionLoaders(tiles.map { it.getBounds() })
        for (i in tiles.indices) {
            val tile = tiles[i]
            val sources = loaders[i]
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
            PhotoMapRegionLoader.Companion.removeUnneededLoaders(allMaps)
        }

        var hasChanges = false
        val parallel = ParallelCoroutineRunner()

        val middleX = tileSources.keys.map { it.x }.average()
        val middleY = tileSources.keys.map { it.y }.average()

        val sortedEntries = tileSources.entries
            .sortedBy { hypot(it.key.x - middleX, it.key.y - middleY) }

        parallel.run(sortedEntries.toList()) { source ->
            if (tileCache.containsKey(source.key) && !alwaysReloadTiles) {
                return@run
            }

            val config = if (backgroundColor.alpha != 255) {
                Bitmap.Config.ARGB_8888
            } else {
                Bitmap.Config.RGB_565
            }

            var canvas: Canvas? = null
            var image: Bitmap? = null

            if (!useFirstImageSize) {
                image = createBitmap(source.key.size.width, source.key.size.height, config)
                image.eraseColor(backgroundColor)
                canvas = Canvas(image)
            }

            source.value.reversed().forEachIndexed { index, loader ->
                val currentImage = loader.load(source.key)?.applyOperationsOrNull(
                    Conditional(
                        index > 0,
                        ReplaceColor(
                            Color.WHITE,
                            Color.argb(127, 127, 127, 127),
                            80f,
                            true,
                            inPlace = true
                        )
                    )
                )

                if (currentImage != null) {
                    if (useFirstImageSize) {
                        image = createBitmap(currentImage.width, currentImage.height, config)
                        canvas = Canvas(image)
                    }

                    canvas?.drawBitmap(currentImage, 0f, 0f, null)
                    currentImage.recycle()
                }
            }

            // Remove transparency
            image = image?.applyOperationsOrNull(
                // Undo color replacement
                Conditional(
                    backgroundColor.alpha != 255 && source.value.size > 1,
                    ReplaceColor(
                        Color.argb(127, 127, 127, 127),
                        Color.WHITE,
                        80f,
                        true,
                        inPlace = true
                    )
                ),
                Convert(config)
            )

            hasChanges = true
            synchronized(lock) {
                if (clearTileWhenNullResponse || image != null) {
                    val old = tileCache[source.key]
                    tileCache += source.key to listOfNotNull(image)
                    old?.forEach { it.recycle() }
                }
            }
            onChange()
        }

        synchronized(lock) {
            tileCache.keys.forEach { key ->
                if (!tileSources.containsKey(key)) {
                    tileCache[key]?.forEach { bitmap -> bitmap.recycle() }
                    hasChanges = true
                }
            }

            tileCache = tileCache.filterKeys { it in tileSources.keys }
        }
        onChange()

        if (hasChanges) {
            System.gc()
            val memoryUsage = tileCache.values.sumOf { bitmaps ->
                bitmaps.sumOf { it.allocationByteCount }
            }
            Log.d("TileLoader", "Tile memory usage: ${memoryUsage / 1024} KB (${tiles.size} tiles)")
        }
    }
}