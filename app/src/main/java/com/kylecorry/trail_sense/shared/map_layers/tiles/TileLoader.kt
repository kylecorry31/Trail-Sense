package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import com.kylecorry.andromeda.bitmaps.BitmapUtils.use
import com.kylecorry.andromeda.bitmaps.operations.Resize
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.andromeda_temp.Pad
import com.kylecorry.trail_sense.shared.map_layers.tiles.infrastructure.persistance.PersistentTileCache
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource

class TileLoader(
    private val source: TileSource,
    private val tileQueue: TileQueue,
    private val padding: Int = 0,
    private val tag: String? = null,
    private val key: String? = null
) {

    val tileCache = TileCache()
    private val persistentCache =
        if (key != null) getAppService<PersistentTileCache>() else null
    private val neighborPaint = Paint().apply {
        isFilterBitmap = false
        isAntiAlias = false
    }
    private val neighborOffsets = listOf(
        -1 to -1,
        0 to -1,
        1 to -1,
        -1 to 0,
        1 to 0,
        -1 to 1,
        0 to 1,
        1 to 1
    )

    fun clearCache() {
        tileCache.clear()
    }

    suspend fun loadTiles(tiles: List<Tile>) = onDefault {
        val tilesSet = tiles.toSet()
        val tilesToLoad = tiles.filter { !tileCache.contains(it) }
        val z = tiles.firstOrNull()?.z

        var hasChanges = false

        // Remove unneeded tiles at the same z index
        if (z != null) {
            val removed = tileCache.removeOtherThan(tilesSet, z)
            hasChanges = hasChanges || removed
        }

        // Create image tiles and enqueue them
        val imageTiles = tilesToLoad.map { tile ->
            ImageTile(
                key = "${tag}_${tile.x}_${tile.y}_${tile.z}",
                tile = tile
            ) {
                val image = loadTile(source, tile)
                image?.applyOperationsOrNull(
                    Resize(
                        tile.size,
                        exact = false
                    ),
                    Pad(
                        padding,
                        if (image.config == Bitmap.Config.ARGB_8888) Color.TRANSPARENT else Color.WHITE
                    )
                )
            }
        }

        // TODO: This should just be used to populate borders, caching should happen before enqueue
        // TODO: This should be handled by a higher level component
        tileQueue.setChangeListener { imageTile ->
            processLoadedTile(imageTile.tile, imageTile.image)
            hasChanges = true
        }

        imageTiles.forEach { tileQueue.enqueue(it) }

        for (tile in tileCache.keys()) {
            tryOrLog {
                populateBorder(tile)
            }
        }

        // TODO: This is no longer correct - need to switch to an LruCache
        val removed = tileCache.removeOtherThan(tilesSet)
        hasChanges = hasChanges || removed

        if (hasChanges) {
            val memoryUsage = tileCache.getMemoryAllocation()
            Log.d(
                "TileLoader",
                "Tile memory usage ($tag): ${memoryUsage / 1024} KB (${tiles.size} tiles)"
            )
        }
    }

    private fun processLoadedTile(tile: Tile, image: Bitmap?) {
        if (image == null) {
            tileCache.remove(tile)
        } else {
            tileCache.put(tile, image)
            tryOrLog {
                populateBorderAndNeighbors(tile)
            }
        }
    }

    private suspend fun loadTile(sourceSelector: TileSource, tile: Tile): Bitmap? {
        val cacheKey = key
        if (cacheKey != null && persistentCache != null) {
            return try {
                persistentCache.getOrPut(cacheKey, tile) {
                    sourceSelector.loadTile(tile) ?: throw NoSuchElementException()
                }
            } catch (e: NoSuchElementException) {
                null
            }
        }
        return sourceSelector.loadTile(tile)
    }

    private fun populateBorderAndNeighbors(tile: Tile) {
        if (padding <= 0) {
            return
        }

        populateBorder(tile)

        neighborOffsets.forEach { (dx, dy) ->
            val neighborTile = tile.getNeighbor(dx, dy)
            populateBorder(neighborTile)
        }
    }

    private fun populateBorder(tile: Tile) {
        if (padding <= 0) {
            return
        }
        tileCache.getLocked(tile) { bitmap ->
            tryOrNothing {
                fillNeighborPixels(tile, bitmap)
            }
        }
    }

    private fun fillNeighborPixels(tile: Tile, originalBitmap: Bitmap) {
        val borderSize = padding
        if (borderSize <= 0) {
            return
        }

        val fallback =
            if (originalBitmap.config != Bitmap.Config.ARGB_8888) originalBitmap else null
        val canvas = Canvas(originalBitmap)
        val w = originalBitmap.width
        val h = originalBitmap.height

        val topTile = tile.getNeighbor(0, -1)
        drawNeighbor(
            canvas,
            topTile,
            borderSize,
            0,
            w,
            borderSize,
            borderSize,
            h - borderSize * 2,
            fallback,
            Rect(borderSize, borderSize, w - borderSize, borderSize + 1)
        )

        val bottomTile = tile.getNeighbor(0, 1)
        drawNeighbor(
            canvas,
            bottomTile,
            borderSize,
            h - borderSize,
            w,
            borderSize,
            borderSize,
            borderSize,
            fallback,
            Rect(borderSize, h - borderSize - 1, w - borderSize, h - borderSize)
        )

        val leftTile = tile.getNeighbor(-1, 0)
        drawNeighbor(
            canvas,
            leftTile,
            0,
            borderSize,
            borderSize,
            h,
            w - borderSize * 2,
            borderSize,
            fallback,
            Rect(borderSize, borderSize, borderSize + 1, h - borderSize)
        )

        val rightTile = tile.getNeighbor(1, 0)
        drawNeighbor(
            canvas,
            rightTile,
            w - borderSize,
            borderSize,
            borderSize,
            h,
            borderSize,
            borderSize,
            fallback,
            Rect(w - borderSize - 1, borderSize, w - borderSize, h - borderSize)
        )

        val topLeftTile = tile.getNeighbor(-1, -1)
        drawNeighbor(
            canvas,
            topLeftTile,
            0,
            0,
            borderSize,
            borderSize,
            w - borderSize * 2,
            h - borderSize * 2,
            fallback,
            Rect(borderSize, borderSize, borderSize + 1, borderSize + 1)
        )

        val topRightTile = tile.getNeighbor(1, -1)
        drawNeighbor(
            canvas,
            topRightTile,
            w - borderSize,
            0,
            borderSize,
            borderSize,
            borderSize,
            h - borderSize * 2,
            fallback,
            Rect(w - borderSize - 1, borderSize, w - borderSize, borderSize + 1)
        )

        val bottomLeftTile = tile.getNeighbor(-1, 1)
        drawNeighbor(
            canvas,
            bottomLeftTile,
            0,
            h - borderSize,
            borderSize,
            borderSize,
            w - borderSize * 2,
            borderSize,
            fallback,
            Rect(borderSize, h - borderSize - 1, borderSize + 1, h - borderSize)
        )

        val bottomRightTile = tile.getNeighbor(1, 1)
        drawNeighbor(
            canvas,
            bottomRightTile,
            w - borderSize,
            h - borderSize,
            borderSize,
            borderSize,
            borderSize,
            borderSize,
            fallback,
            Rect(w - borderSize - 1, h - borderSize - 1, w - borderSize, h - borderSize)
        )
    }

    private fun drawNeighbor(
        canvas: Canvas,
        neighborTile: Tile,
        destX: Int,
        destY: Int,
        destWidth: Int,
        destHeight: Int,
        srcXStart: Int,
        srcYStart: Int,
        fallbackBitmap: Bitmap? = null,
        fallbackSrcRect: Rect? = null
    ) {
        tryOrNothing {
            val neighborBitmap = tileCache.get(neighborTile)

            if (neighborBitmap == null) {
                if (fallbackBitmap != null && fallbackSrcRect != null) {
                    val destRect = Rect(
                        destX,
                        destY,
                        destX + destWidth,
                        destY + destHeight
                    )
                    fallbackBitmap.copy(fallbackBitmap.config ?: Bitmap.Config.ARGB_8888, false)
                        .use {
                            canvas.drawBitmap(
                                this,
                                fallbackSrcRect,
                                destRect,
                                neighborPaint
                            )
                        }
                }
                return
            }
            val srcRect = Rect(
                srcXStart,
                srcYStart,
                srcXStart + destWidth,
                srcYStart + destHeight
            )

            val destRect = Rect(
                destX,
                destY,
                destX + destWidth,
                destY + destHeight
            )

            canvas.drawBitmap(
                neighborBitmap,
                srcRect,
                destRect,
                neighborPaint
            )
        }
    }
}