package com.kylecorry.trail_sense.shared.andromeda_temp

import android.graphics.Bitmap
import com.kylecorry.andromeda.bitmaps.operations.BitmapOperation
import kotlinx.coroutines.runBlocking

class MapPixels(private val inPlace: Boolean = false, private val map: (Int) -> Int) :
    BitmapOperation {
    override fun execute(bitmap: Bitmap): Bitmap {
        val pixels = bitmap.getPixels()
        runBlocking {
            pixels.forEachParallel { i ->
                pixels[i] = map(pixels[i])
            }
        }
        if (inPlace && bitmap.isMutable) {
            bitmap.setPixels(pixels)
            return bitmap
        }
        return Bitmap.createBitmap(
            pixels,
            bitmap.width,
            bitmap.height,
            bitmap.config ?: Bitmap.Config.ARGB_8888
        )
    }

    private suspend inline fun IntArray.forEachParallel(crossinline action: (Int) -> Unit) {
        return parallelForEachIndex(this.size, action = action)
    }

}