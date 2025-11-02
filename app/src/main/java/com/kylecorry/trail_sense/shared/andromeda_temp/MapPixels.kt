package com.kylecorry.trail_sense.shared.andromeda_temp

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import com.kylecorry.andromeda.bitmaps.operations.BitmapOperation

class MapPixels(private val inPlace: Boolean = false, private val map: (Int) -> Int) :
    BitmapOperation {
    override fun execute(bitmap: Bitmap): Bitmap {
        val pixels = bitmap.getPixels()
        pixels.forEachParallel { i ->
            pixels[i] = map(pixels[i])
        }
        if (inPlace) {
            bitmap.setPixels(pixels)
            return bitmap
        }
        val newBitmap =
            createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        newBitmap.setPixels(pixels)
        return newBitmap
    }

    private inline fun IntArray.forEachParallel(crossinline action: (Int) -> Unit) {
        return parallelForEachIndex(this.size, action = action)
    }

}