package com.kylecorry.trail_sense.shared.canvas.tiles

import android.graphics.RectF

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
}

