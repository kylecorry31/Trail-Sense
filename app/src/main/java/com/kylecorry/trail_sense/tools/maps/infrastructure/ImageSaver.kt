package com.kylecorry.trail_sense.tools.maps.infrastructure

import android.graphics.Bitmap
import android.os.Build
import java.io.OutputStream

class ImageSaver {

    fun save(image: Bitmap, stream: OutputStream, quality: Int = 90) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (quality == 100) {
                image.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 50, stream)
            } else {
                image.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, stream)
            }
        } else {
            image.compress(Bitmap.CompressFormat.WEBP, quality, stream)
        }
    }

}