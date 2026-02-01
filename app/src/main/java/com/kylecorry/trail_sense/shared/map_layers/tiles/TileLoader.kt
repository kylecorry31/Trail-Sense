package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import androidx.core.os.bundleOf
import com.kylecorry.andromeda.bitmaps.BitmapUtils.use
import com.kylecorry.andromeda.bitmaps.operations.Resize
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.andromeda_temp.Pad
import com.kylecorry.trail_sense.shared.map_layers.tiles.infrastructure.persistance.PersistentTileCache
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import java.time.Instant

class TileLoader(
    private val source: TileSource,
    private val tileQueue: TileQueue,
    private val padding: Int = 0,
    private val tag: String? = null,
    private val key: String? = null,
    private val updateListener: () -> Unit = {}
) {

    val tileCache = TileCache(tag ?: "", 512)

    private val persistentCache =
        if (key != null) getAppService<PersistentTileCache>() else null
    private val neighborPaint = Paint().apply {
        isFilterBitmap = false
        isAntiAlias = false
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
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

    init {
        // TODO: This should be handled by a higher level component
        tileQueue.setChangeListener { imageTile ->
            tryOrLog {
                populateBorderAndNeighbors(imageTile)
            }
            updateListener()
        }
    }

    fun clearCache() {
        tileCache.evictAll()
    }

    fun loadTiles(tiles: List<Tile>, time: Instant) {
        val imageTiles = tiles.map { tile ->
            val key = "${tag}_${tile.x}_${tile.y}_${tile.z}"
            val newTile = tileCache.getOrPut(key) {
                ImageTile(
                    key = key,
                    tile = tile,
                    loadFunction = {
                        loadTile(source, tile, time)
                    }
                )
            }
            // Replace the load function every cycle to ensure it uses the latest parameters
            newTile.setLoader {
                loadTile(source, tile, time)
            }
            newTile
        }

        imageTiles.forEach { tileQueue.enqueue(it) }
    }

    private suspend fun loadTile(sourceSelector: TileSource, tile: Tile, time: Instant): Bitmap? {
        val params = bundleOf(TileSource.PARAM_TIME to time.toEpochMilli())
        val cacheKey = key
        val existing = if (cacheKey != null && persistentCache != null) {
            try {
                persistentCache.getOrPut(cacheKey, tile) {
                    sourceSelector.loadTile(tile, params) ?: throw NoSuchElementException()
                }
            } catch (_: NoSuchElementException) {
                null
            }
        } else {
            sourceSelector.loadTile(tile, params)
        }
        return existing?.applyOperationsOrNull(
            Resize(
                tile.size,
                exact = false
            ),
            Pad(
                padding,
                if (existing.config == Bitmap.Config.ARGB_8888) Color.TRANSPARENT else Color.WHITE
            )
        )
    }

    private fun populateBorderAndNeighbors(tile: ImageTile) {
        if (padding <= 0) {
            return
        }

        populateBorder(tile.tile)

        neighborOffsets.forEach { (dx, dy) ->
            val neighborTile = tile.tile.getNeighbor(dx, dy)
            populateBorder(neighborTile)
        }
    }

    private fun populateBorder(tile: Tile) {
        if (padding <= 0) {
            return
        }
        tileCache.peek(tile)?.withImage { bitmap ->
            bitmap ?: return@withImage
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
            tileCache.peek(neighborTile)?.withImage { neighborBitmap ->
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
                    return@withImage
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

}