package com.kylecorry.trail_sense.weather.infrastructure.clouds

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.convolve
import com.kylecorry.sol.science.meteorology.clouds.CloudShape
import com.kylecorry.trail_sense.weather.domain.clouds.BGIsSkySpecification
import com.kylecorry.trail_sense.weather.domain.clouds.CloudService
import com.kylecorry.trail_sense.weather.domain.clouds.SaturationIsObstacleSpecification

class CloudAnalyzer(
    private val skyDetectionSensitivity: Int,
    private val obstacleRemovalSensitivity: Int,
    private val skyColorOverlay: Int,
    private val excludedColorOverlay: Int,
    private val cloudColorOverlay: Int,
) {

    private val cloudService = CloudService()

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    suspend fun getClouds(
        bitmap: Bitmap,
        setPixel: (x: Int, y: Int, pixel: Int) -> Unit = { _, _, _ -> }
    ): CloudObservation {
        var bluePixels = 0
        var cloudPixels = 0
        var luminance = 0.0

        val isSky = BGIsSkySpecification(100 - skyDetectionSensitivity)

        val isObstacle = SaturationIsObstacleSpecification(1 - obstacleRemovalSensitivity / 100f)

        val edges = bitmap.convolve(floatArrayOf(-0.99f, -0.99f, -0.99f, -0.99f, 8f, -0.99f, -0.99f, -0.99f, -0.99f))
        var totalContrast = 0.0

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
                        setPixel(w, h, cloudColorOverlay)
                        totalContrast += Color.blue(edges.getPixel(w, h))
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

        val shape = when {
            cover > 0.85f -> {
                CloudShape.Strato
            }
            cover > 0.1f -> {
                CloudShape.Cumulo
            }
            else -> {
                null
            }
        }

        val contrast = if (cloudPixels != 0){
            (totalContrast / cloudPixels).toFloat() / 255
        } else {
            0f
        }

        // TODO: Determine height by contrast

        val clouds = if (shape != null) cloudService.getCloudsWithShape(shape)
            .sortedBy { it.height } else emptyList()

        return CloudObservation(
            cover,
            luminance.toFloat(),
            contrast,
            clouds
        )
    }


    private fun average(@ColorInt color: Int): Float {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        return (r + g + b).toFloat() / 3f
    }

}