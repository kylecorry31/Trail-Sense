package com.kylecorry.trail_sense.shared.bitmaps

import android.graphics.Bitmap

interface BitmapOperation {
    fun execute(bitmap: Bitmap): Bitmap
}