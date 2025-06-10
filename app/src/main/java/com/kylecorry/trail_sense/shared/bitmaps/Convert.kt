package com.kylecorry.trail_sense.shared.bitmaps

import android.graphics.Bitmap

class Convert(private val config: Bitmap.Config) : BitmapOperation {
    override fun execute(bitmap: Bitmap): Bitmap {
        if (bitmap.config == config) {
            return bitmap
        }
        return bitmap.copy(config, bitmap.isMutable)
    }
}