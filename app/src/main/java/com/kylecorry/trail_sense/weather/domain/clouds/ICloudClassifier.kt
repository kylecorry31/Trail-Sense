package com.kylecorry.trail_sense.weather.domain.clouds

import android.graphics.Bitmap
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus

interface ICloudClassifier {

    suspend fun classify(
        bitmap: Bitmap,
        setPixel: (x: Int, y: Int, classification: SkyPixelClassification) -> Unit = { _, _, _ -> }
    ): List<ClassificationResult<CloudGenus>>

}