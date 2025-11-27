package com.kylecorry.trail_sense.shared.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.util.Size
import androidx.core.graphics.createBitmap
import com.kylecorry.andromeda.bitmaps.BitmapUtils.use

class TiledImageReader(
    private val tiles: List<Pair<Rect, ImageReader>>,
) : ImageReader {

    private var cachedSize: Size? = null

    override fun getSize(): Size {
        if (cachedSize != null) {
            return cachedSize!!
        }

        var left = 0
        var top = 0
        var right = 0
        var bottom = 0
        for (tile in tiles) {
            left = minOf(left, tile.first.left)
            top = minOf(top, tile.first.top)
            right = maxOf(right, tile.first.right)
            bottom = maxOf(bottom, tile.first.bottom)
        }
        val size = Size(right - left, bottom - top)
        cachedSize = size
        return size
    }

    override suspend fun getRegion(bounds: Rect, config: Bitmap.Config): Bitmap? {
        val images = tiles.filter {
            it.first.intersects(
                bounds.left,
                bounds.top,
                bounds.right,
                bounds.bottom
            )
        }
        if (images.isEmpty()) {
            return null
        }
        val result = createBitmap(bounds.width(), bounds.height(), config)
        val canvas = Canvas(result)
        for (image in images) {
            val intersection = Rect()
            if (intersection.setIntersect(bounds, image.first)) {
                val region = Rect(
                    intersection.left - image.first.left,
                    intersection.top - image.first.top,
                    intersection.right - image.first.left,
                    intersection.bottom - image.first.top
                )
                image.second.getRegion(region)?.use {
                    val destRect = Rect(
                        intersection.left - bounds.left,
                        intersection.top - bounds.top,
                        intersection.right - bounds.left,
                        intersection.bottom - bounds.top
                    )
                    val srcRect = Rect(0, 0, width, height)
                    canvas.drawBitmap(this, srcRect, destRect, null)
                }
            }
        }
        return result
    }
}