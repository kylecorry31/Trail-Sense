package com.kylecorry.trail_sense.shared.andromeda_temp

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.graphics.createBitmap
import com.kylecorry.andromeda.bitmaps.operations.BitmapOperation

class Flip(private val vertical: Boolean = true, private val horizontal: Boolean = true) :
    BitmapOperation {

    override fun execute(bitmap: Bitmap): Bitmap {
        val newBitmap =
            createBitmap(
                bitmap.width,
                bitmap.height,
                bitmap.config ?: Bitmap.Config.ARGB_8888
            )
        val canvas = Canvas(newBitmap)
        val scaleX = if (horizontal) -1f else 1f
        val scaleY = if (vertical) -1f else 1f
        val pivotX = bitmap.width / 2f
        val pivotY = bitmap.height / 2f
        canvas.scale(scaleX, scaleY, pivotX, pivotY)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        return newBitmap
    }
}