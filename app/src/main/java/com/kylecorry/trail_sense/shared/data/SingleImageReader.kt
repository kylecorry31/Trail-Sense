package com.kylecorry.trail_sense.shared.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Size
import com.kylecorry.andromeda.bitmaps.BitmapUtils
import com.kylecorry.andromeda.bitmaps.BitmapUtils.use
import com.kylecorry.andromeda.core.coroutines.onIO

class SingleImageReader(
    private val imageSize: Size,
    private val streamable: InputStreamable,
) : ImageReader {
    override fun getSize(): Size {
        return imageSize
    }

    override suspend fun getRegion(bounds: Rect, config: Bitmap.Config): Bitmap? = onIO {
        val image = streamable.getInputStream() ?: return@onIO null
        try {
            val rect = BitmapUtils.getExactRegion(bounds, imageSize)
            val bitmap = BitmapUtils.decodeRegion(
                image,
                rect,
                BitmapFactory.Options().also { it.inPreferredConfig = config },
                autoClose = false
            ) ?: return@onIO null

            if (bitmap.width <= 0 || bitmap.height <= 0) {
                bitmap.recycle()
                return@onIO null
            }

            // Crop the bitmap to the original region
            if (rect != bounds) {
                var cropped: Bitmap? = null
                bitmap.use {
                    // TODO: Handle when the original region was out of bounds (fill with 0)
                    cropped = Bitmap.createBitmap(
                        bitmap,
                        bounds.left - rect.left,
                        bounds.top - rect.top,
                        bounds.width(),
                        bounds.height()
                    )
                }
                cropped
            } else {
                bitmap
            }
        } finally {
            image.close()
        }
    }
}