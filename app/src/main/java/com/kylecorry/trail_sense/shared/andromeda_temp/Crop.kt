package com.kylecorry.trail_sense.shared.andromeda_temp

import android.graphics.Bitmap
import com.kylecorry.andromeda.bitmaps.BitmapUtils.crop
import com.kylecorry.andromeda.bitmaps.operations.BitmapOperation

class Crop(
    private val x: Float,
    private val y: Float,
    private val width: Float,
    private val height: Float
) : BitmapOperation {

    override fun execute(bitmap: Bitmap): Bitmap {
        return bitmap.crop(x, y, width, height)
    }
}