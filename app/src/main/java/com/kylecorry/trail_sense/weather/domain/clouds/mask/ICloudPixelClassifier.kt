package com.kylecorry.trail_sense.weather.domain.clouds.mask

import androidx.annotation.ColorInt

interface ICloudPixelClassifier {
    fun classify(@ColorInt pixel: Int): SkyPixelClassification
}