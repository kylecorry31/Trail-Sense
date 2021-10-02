package com.kylecorry.trail_sense.weather.infrastructure.clouds

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.ColorInt
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
    suspend fun getClouds(bitmap: Bitmap, setPixel: (x: Int, y: Int, pixel: Int) -> Unit = {_, _, _ ->}): CloudObservation {
        var bluePixels = 0
        var cloudPixels = 0
        var luminance = 0.0

        val isSky = BGIsSkySpecification(100 - skyDetectionSensitivity)

        val isObstacle = SaturationIsObstacleSpecification(1 - obstacleRemovalSensitivity / 100f)

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
                        luminance += average(pixel)
                        setPixel(w, h, cloudColorOverlay)
                    }
                }

            }
        }

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

        // TODO: Replace this with a logistic regression model
        val type = when {
            cover > 0.85f && luminance > 0.4f -> {
                CloudType.Stratus
            }
            cover > 0.85f -> {
                CloudType.Nimbostratus
            }
            cover > 0.1f && luminance > 0.4f -> {
                CloudType.Cumulus
            }
            cover > 0.1f -> {
                CloudType.Cumulonimbus
            }
            else -> {
                null
            }
        }

        return CloudObservation(
            cover,
            luminance.toFloat(),
            listOfNotNull(type)
        )
    }


    private fun average(@ColorInt color: Int): Float {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        return (r + g + b).toFloat() / 3f
    }

}