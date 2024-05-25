package com.kylecorry.trail_sense.shared.canvas.tiles

import android.graphics.Bitmap
import android.graphics.RectF

interface TileLoader {
    val tiles: List<Pair<ImageTile, Bitmap>>

    // TODO: There are gaps between tiles
    suspend fun updateTiles(zoom: Float, clipBounds: RectF)
    fun release()
}