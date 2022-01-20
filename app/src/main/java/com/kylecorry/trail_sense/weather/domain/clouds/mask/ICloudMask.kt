package com.kylecorry.trail_sense.weather.domain.clouds.mask

import android.graphics.Bitmap

interface ICloudMask {
    fun mask(input: Bitmap, output: Bitmap? = null): Bitmap
}