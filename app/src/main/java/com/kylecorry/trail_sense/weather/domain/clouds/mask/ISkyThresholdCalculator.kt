package com.kylecorry.trail_sense.weather.domain.clouds.mask

import android.graphics.Bitmap

interface ISkyThresholdCalculator {
    suspend fun getThreshold(bitmap: Bitmap): Int
}