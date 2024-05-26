package com.kylecorry.trail_sense.shared.canvas.tiles

import android.graphics.RectF
import android.util.Size
import kotlin.math.ceil
import kotlin.math.log
import kotlin.math.pow

object TileCreator {

    fun createTiles(width: Int, height: Int, tileSize: Int): List<ImageTile> {
        val tiles = mutableListOf<ImageTile>()
        for (x in 0 until width step tileSize) {
            for (y in 0 until height step tileSize) {
                val tileWidth = if (x + tileSize > width) width - x else tileSize
                val tileHeight = if (y + tileSize > height) height - y else tileSize
                tiles.add(ImageTile(x, y, tileWidth, tileHeight))
            }
        }
        return tiles
    }

    fun clip(tiles: List<ImageTile>, rect: RectF): List<ImageTile> {
        return tiles.filter { tile ->
            val tileRect = RectF(
                tile.x.toFloat(),
                tile.y.toFloat(),
                (tile.x + tile.width).toFloat(),
                (tile.y + tile.height).toFloat()
            )
            tileRect.intersect(rect)
        }
    }

    fun getTileSize(
        sourceWidth: Int,
        sourceHeight: Int,
        scale: Float,
        desiredTileWidth: Int = 256,
        minimumSize: Int = 256
    ): Size {
        val zoomedWidth = sourceWidth * scale
        val zoomedHeight = sourceHeight * scale
        val numTilesX = ceil((zoomedWidth + desiredTileWidth - 1) / desiredTileWidth).toInt()
        val numTilesY = ceil((zoomedHeight + desiredTileWidth - 1) / desiredTileWidth).toInt()
        val tileSizeX = nextPowerOf2(sourceWidth / numTilesX)
        val tileSizeY = nextPowerOf2(sourceHeight / numTilesY)
        return Size(
            tileSizeX.coerceAtLeast(minimumSize),
            tileSizeY.coerceAtLeast(minimumSize)
        )
    }

    private fun nextPowerOf2(value: Int): Int {
        if (value <= 0) return 1
        // TODO: Use bitwise operators to determine if the number is a power of 2
        return 2.0.pow(ceil(log(value.toDouble(), 2.0))).toInt()
    }
}

