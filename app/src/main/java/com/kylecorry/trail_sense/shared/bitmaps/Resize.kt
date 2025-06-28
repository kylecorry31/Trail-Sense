package com.kylecorry.trail_sense.shared.bitmaps

import android.graphics.Bitmap
import android.util.Size
import com.kylecorry.andromeda.bitmaps.BitmapUtils.resizeExact
import com.kylecorry.andromeda.bitmaps.BitmapUtils.resizeToFit

class Resize(private val size: Size, private val exact: Boolean = true) : BitmapOperation {
    override fun execute(bitmap: Bitmap): Bitmap {
        return if (exact) {
            if (bitmap.width == size.width && bitmap.height == size.height) {
                bitmap
            } else {
                bitmap.resizeExact(size.width, size.height)
            }
        } else {
            if (bitmap.width <= size.width && bitmap.height <= size.height) {
                bitmap
            } else {
                bitmap.resizeToFit(size.width, size.height)
            }
        }
    }
}