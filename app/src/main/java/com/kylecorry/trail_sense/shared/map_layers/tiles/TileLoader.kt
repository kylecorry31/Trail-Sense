package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import com.kylecorry.andromeda.bitmaps.operations.Resize
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.trail_sense.shared.andromeda_temp.Pad
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource

class TileLoader(private val padding: Int = 0) {

    val tileCache = TileCache()
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

    suspend fun loadTiles(
        sourceSelector: TileSource,
        tiles: List<Tile>
    ) = onDefault {
        val tilesSet = tiles.toSet()
        val tilesToLoad = tiles.filter { !tileCache.contains(it) }

        var hasChanges = false

        sourceSelector.load(tilesToLoad) { tile, image ->
            val resized = image?.applyOperationsOrNull(
                Resize(
                    tile.size,
                    exact = false
                ),
                Pad(
                    padding,
                    if (image.config == Bitmap.Config.ARGB_8888) Color.TRANSPARENT else Color.WHITE
                )
            )
            if (resized == null) {
                tileCache.remove(tile)
            } else {
                tileCache.put(tile, resized)
                tryOrLog {
                    populateBorderAndNeighbors(tile)
                }
            }
            hasChanges = true
        }

        for (tile in tileCache.keys()) {
            tryOrLog {
                populateBorder(tile)
            }
        }

        val removed = tileCache.removeOtherThan(tilesSet)
        hasChanges = hasChanges || removed

        if (hasChanges) {
            val memoryUsage = tileCache.getMemoryAllocation()
            Log.d("TileLoader", "Tile memory usage: ${memoryUsage / 1024} KB (${tiles.size} tiles)")
        }
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
        val bitmap = tileCache.get(tile) ?: return
        fillNeighborPixels(tile, bitmap)
    }

    private fun fillNeighborPixels(tile: Tile, originalBitmap: Bitmap) {
        val borderSize = padding
        if (borderSize <= 0) {
            return
        }

        val canvas = Canvas(originalBitmap)

        val topTile = tile.getNeighbor(0, -1)
        drawNeighbor(
            canvas,
            topTile,
            borderSize,
            0,
            originalBitmap.width,
            borderSize,
            borderSize,
            originalBitmap.height - borderSize * 2
        )

        val bottomTile = tile.getNeighbor(0, 1)
        drawNeighbor(
            canvas,
            bottomTile,
            borderSize,
            originalBitmap.height - borderSize,
            originalBitmap.width,
            borderSize,
            borderSize,
            borderSize
        )

        val leftTile = tile.getNeighbor(-1, 0)
        drawNeighbor(
            canvas,
            leftTile,
            0,
            borderSize,
            borderSize,
            originalBitmap.height,
            originalBitmap.width - borderSize * 2,
            borderSize
        )

        val rightTile = tile.getNeighbor(1, 0)
        drawNeighbor(
            canvas,
            rightTile,
            originalBitmap.width - borderSize,
            borderSize,
            borderSize,
            originalBitmap.height,
            borderSize,
            borderSize
        )

        val topLeftTile = tile.getNeighbor(-1, -1)
        drawNeighbor(
            canvas,
            topLeftTile,
            0,
            0,
            borderSize,
            borderSize,
            originalBitmap.width - borderSize * 2,
            originalBitmap.height - borderSize * 2
        )

        val topRightTile = tile.getNeighbor(1, -1)
        drawNeighbor(
            canvas,
            topRightTile,
            originalBitmap.width - borderSize,
            0,
            borderSize,
            borderSize,
            borderSize,
            originalBitmap.height - borderSize * 2
        )

        val bottomLeftTile = tile.getNeighbor(-1, 1)
        drawNeighbor(
            canvas,
            bottomLeftTile,
            0,
            originalBitmap.height - borderSize,
            borderSize,
            borderSize,
            originalBitmap.width - borderSize * 2,
            borderSize
        )

        val bottomRightTile = tile.getNeighbor(1, 1)
        drawNeighbor(
            canvas,
            bottomRightTile,
            originalBitmap.width - borderSize,
            originalBitmap.height - borderSize,
            borderSize,
            borderSize,
            borderSize,
            borderSize
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
        srcYStart: Int
    ) {
        val neighborBitmap = tileCache.get(neighborTile) ?: return
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