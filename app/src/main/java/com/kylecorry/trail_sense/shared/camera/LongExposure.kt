package com.kylecorry.trail_sense.shared.camera

import android.graphics.Bitmap
import android.graphics.Color
import com.kylecorry.andromeda.bitmaps.BitmapUtils.add

class LongExposure(private val count: Int) {
    private var bitmap: Bitmap? = null
    private var currentCount = 0

    fun addFrame(frame: Bitmap) {
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(frame.width, frame.height, Bitmap.Config.ARGB_8888)
            // Fill alpha channel
//            for (x in 0 until frame.width) {
//                for (y in 0 until frame.height) {
//                    bitmap?.setPixel(x, y, Color.BLACK)
//                }
//            }
            bitmap?.add(frame, 1f, 1 / count.toFloat(), inPlace = true)
        } else {
            bitmap?.add(frame, 1f, 1 / count.toFloat(), inPlace = true)
        }
        currentCount++
    }

    fun getLongExposure(): Bitmap? {
        return bitmap
    }

    fun reset() {
        bitmap = null
        currentCount = 0
    }

    fun isComplete(): Boolean {
        return currentCount >= count
    }
}