package com.kylecorry.trail_sense.tools.clouds.domain.classification

import android.graphics.Bitmap
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus

interface ICloudClassifier {

    suspend fun classify(bitmap: Bitmap): List<ClassificationResult<CloudGenus?>>

}