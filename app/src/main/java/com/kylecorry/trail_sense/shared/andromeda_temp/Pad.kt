package com.kylecorry.trail_sense.shared.andromeda_temp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import com.kylecorry.andromeda.bitmaps.operations.BitmapOperation

class Pad(private val padding: Int, private val color: Int) : BitmapOperation {

    private val paint = Paint().also {
        it.isFilterBitmap = false
        it.isAntiAlias = false
    }

    override fun execute(bitmap: Bitmap): Bitmap {
        if (padding == 0) {
            return bitmap
        }
        val newBitmap =
            createBitmap(
                bitmap.width + 2 * padding,
                bitmap.height + 2 * padding,
                bitmap.config ?: Bitmap.Config.ARGB_8888
            )
        newBitmap.eraseColor(color)
        val canvas = Canvas(newBitmap)
        canvas.drawBitmap(bitmap, padding.toFloat(), padding.toFloat(), paint)
        return newBitmap
    }
}