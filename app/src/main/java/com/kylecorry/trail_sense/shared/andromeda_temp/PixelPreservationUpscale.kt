package com.kylecorry.trail_sense.shared.andromeda_temp

import android.graphics.Bitmap
import android.util.Size
import com.kylecorry.andromeda.bitmaps.BitmapUtils.xbr2xUpscale
import com.kylecorry.andromeda.bitmaps.operations.BitmapOperation

/**
 * An upscaler that preserves the colors of the pixels but attempts to smooth edges
 */
class PixelPreservationUpscale(
    private val size: Size
) : BitmapOperation {
    override fun execute(bitmap: Bitmap): Bitmap {
        var output = bitmap
        var width = bitmap.width
        var height = bitmap.height
        while (width < size.width && height < size.height) {
            val newBitmap = output.xbr2xUpscale()
            if (output != bitmap) {
                output.recycle()
            }
            output = newBitmap
            width = output.width
            height = output.height
        }
        return output
    }
}