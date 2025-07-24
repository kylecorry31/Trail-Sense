package com.kylecorry.trail_sense.shared.bitmaps

import android.graphics.Bitmap
import android.util.Size
import androidx.core.graphics.scale
import com.kylecorry.andromeda.bitmaps.BitmapUtils.resizeExact
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.resizeToFit2

class Resize(
    private val size: Size,
    private val exact: Boolean = true,
    private val useBilinearScaling: Boolean = true,
) : BitmapOperation {
    override fun execute(bitmap: Bitmap): Bitmap {
        val shouldFilter = useBilinearScaling && !(bitmap.width == 1 && bitmap.height == 1)

        return if (exact) {
            if (bitmap.width == size.width && bitmap.height == size.height) {
                bitmap
            } else if (shouldFilter) {
                bitmap.resizeExact(size.width, size.height)
            } else {
                bitmap.scale(size.width, size.height, false)
            }
        } else {
            if (bitmap.width <= size.width && bitmap.height <= size.height) {
                bitmap
            } else {
                bitmap.resizeToFit2(size.width, size.height, shouldFilter)
            }
        }
    }
}