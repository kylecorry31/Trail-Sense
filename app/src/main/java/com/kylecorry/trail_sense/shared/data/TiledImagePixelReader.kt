package com.kylecorry.trail_sense.shared.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.util.Size
import androidx.core.graphics.createBitmap
import com.kylecorry.andromeda.bitmaps.BitmapUtils.use
import java.io.InputStream

class DataImage(
    val bounds: Rect,
    val config: Bitmap.Config = Bitmap.Config.ARGB_8888,
    val lookupOrder: Int = 0,
    val streamProducer: suspend () -> InputStream?,
) {
    // TODO: This should take in a stream producer
    val reader = ImagePixelReader2(
        Size(bounds.width(), bounds.height()),
        config,
        lookupOrder = lookupOrder,
        returnAllPixels = true
    )
}

class TiledImagePixelReader(
    private val images: List<DataImage>,
    private val config: Bitmap.Config = Bitmap.Config.ARGB_8888
) {
    suspend fun loadRegion(bounds: Rect): Bitmap? {
        val images = images.filter { it.bounds.intersect(bounds) }
        if (images.isEmpty()) {
            return null
        }
        val result = createBitmap(bounds.width(), bounds.height(), config)
        for (image in images) {
            val intersection = Rect()
            if (intersection.setIntersect(bounds, image.bounds)) {
                val region = Rect(
                    intersection.left - image.bounds.left,
                    intersection.top - image.bounds.top,
                    intersection.right - image.bounds.left,
                    intersection.bottom - image.bounds.top
                )
                val stream = image.streamProducer() ?: continue
                image.reader.getRegion(
                    stream,
                    region,
                    exact = true,
                    autoClose = true
                )?.second?.use {
                    val destRect = Rect(
                        intersection.left - bounds.left,
                        intersection.top - bounds.top,
                        intersection.right - bounds.left,
                        intersection.bottom - bounds.top
                    )
                    val srcRect = Rect(0, 0, width, height)
                    Canvas(result).drawBitmap(this, srcRect, destRect, null)
                }
            }
        }
        return result
    }

}