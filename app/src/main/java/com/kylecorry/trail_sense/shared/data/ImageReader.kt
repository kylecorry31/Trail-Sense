package com.kylecorry.trail_sense.shared.data

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Size

interface ImageReader {
    fun getSize(): Size
    suspend fun getRegion(bounds: Rect, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap?
}