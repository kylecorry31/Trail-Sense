package com.kylecorry.trail_sense.shared.bitmaps

import android.graphics.Bitmap
import com.kylecorry.andromeda.bitmaps.BitmapUtils.threshold
import com.kylecorry.andromeda.bitmaps.ColorChannel

class Threshold(
    private val threshold: Float,
    private val binary: Boolean = true,
    private val channel: ColorChannel? = null
) : BitmapOperation {
    override fun execute(bitmap: Bitmap): Bitmap {
        return bitmap.threshold(threshold, binary, channel)
    }
}