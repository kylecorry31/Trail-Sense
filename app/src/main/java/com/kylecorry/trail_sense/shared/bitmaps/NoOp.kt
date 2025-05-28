package com.kylecorry.trail_sense.shared.bitmaps

import android.graphics.Bitmap

class NoOp : BitmapOperation {
    override fun execute(bitmap: Bitmap): Bitmap {
        return bitmap
    }
}