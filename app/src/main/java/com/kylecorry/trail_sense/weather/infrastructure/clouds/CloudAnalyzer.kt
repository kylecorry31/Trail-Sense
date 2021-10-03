package com.kylecorry.trail_sense.weather.infrastructure.clouds

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.ColorInt
import com.google.android.renderscript.Toolkit
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.map
import com.kylecorry.sol.math.classifiers.LogisticRegressionClassifier
import com.kylecorry.sol.science.meteorology.clouds.CloudType
import com.kylecorry.trail_sense.weather.domain.clouds.BGIsSkySpecification
import com.kylecorry.trail_sense.weather.domain.clouds.SaturationIsObstacleSpecification

class CloudAnalyzer(
    private val skyDetectionSensitivity: Int,
    private val obstacleRemovalSensitivity: Int,
    private val skyColorOverlay: Int,
    private val excludedColorOverlay: Int,
    private val cloudColorOverlay: Int,
) {

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    suspend fun getClouds(
        bitmap: Bitmap,
        setPixel: (x: Int, y: Int, pixel: Int) -> Unit = { _, _, _ -> }
    ): CloudObservation {
        var bluePixels = 0
        var cloudPixels = 0
        var luminance = 0.0
        var totalContrast = 0.0

        val isSky = BGIsSkySpecification(100 - skyDetectionSensitivity)

        val isObstacle = SaturationIsObstacleSpecification(1 - obstacleRemovalSensitivity / 100f)

        val edges = Toolkit.convolve(bitmap, floatArrayOf(-0.99f, -0.99f, -0.99f, -0.99f, 8f, -0.99f, -0.99f, -0.99f, -0.99f))

        for (w in 0 until bitmap.width) {
            for (h in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(w, h)

                when {
                    isSky.isSatisfiedBy(pixel) -> {
                        bluePixels++
                        setPixel(w, h, skyColorOverlay)
                    }
                    isObstacle.isSatisfiedBy(pixel) -> {
                        setPixel(w, h, excludedColorOverlay)
                    }
                    else -> {
                        cloudPixels++
                        val lum = average(pixel)
                        luminance += lum
                        totalContrast += Color.blue(edges.getPixel(w, h))
                        setPixel(w, h, cloudColorOverlay)
                    }
                }
            }
        }

        edges.recycle()

        val cover = if (bluePixels + cloudPixels != 0) {
            cloudPixels / (bluePixels + cloudPixels).toFloat()
        } else {
            0f
        }

        luminance = if (cloudPixels != 0) {
            luminance / cloudPixels
        } else {
            0.0
        }


        val contrast = if (cloudPixels != 0){
            map(totalContrast / cloudPixels, 0.0, 255.0, 0.0, 1.0).toFloat()
        } else {
            0f
        }

        val weights = arrayOf(
            arrayOf(0.38244242f, 0.40601874f, -0.42202838f, -0.1622278f),
            arrayOf(0.47629045f, -0.45307762f, 0.71279418f, -0.45163563f),
            arrayOf(-0.26328539f, 0.03031462f, 0.05058717f, 0.30928571f)
        )
        val classifier = LogisticRegressionClassifier(weights)
        val confidences = classifier.classify(listOf(cover, luminance.toFloat(), 1f))
        val prediction = SolMath.argmax(confidences)

        // TODO: Replace this with a logistic regression model
//        val type = when {
//            cover > 0.85f && luminance > 0.4f -> {
//                CloudType.Stratus
//            }
//            cover > 0.85f -> {
//                CloudType.Nimbostratus
//            }
//            cover > 0.1f && luminance > 0.4f -> {
//                CloudType.Cumulus
//            }
//            cover > 0.1f -> {
//                CloudType.Cumulonimbus
//            }
//            else -> {
//                null
//            }
//        }

        val type = when (prediction) {
            0 -> CloudType.Stratus
            1 -> CloudType.Nimbostratus
            2 -> CloudType.Cumulus
            else -> CloudType.Cumulonimbus
        }

        return CloudObservation(
            cover,
            luminance.toFloat(),
            contrast,
            type,
            confidences[prediction]
        )
    }


    private fun average(@ColorInt color: Int): Float {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        return (r + g + b).toFloat() / 3f
    }

}