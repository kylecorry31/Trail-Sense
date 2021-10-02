package com.kylecorry.trail_sense.weather.infrastructure.clouds

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.set
import com.kylecorry.sol.math.SolMath.power
import com.kylecorry.sol.math.statistics.StatisticsService
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
    suspend fun getFeatures(bitmap: Bitmap, out: Bitmap?, bitmask: Boolean): CloudFeatures {
        var bluePixels = 0
        var cloudPixels = 0

        val blueCloudPixels = mutableListOf<Float>()

        val isSky = BGIsSkySpecification(100 - skyDetectionSensitivity)

        val isObstacle = SaturationIsObstacleSpecification(1 - obstacleRemovalSensitivity / 100f)

        for (w in 0 until bitmap.width) {
            for (h in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(w, h)

                if (isSky.isSatisfiedBy(pixel)) {
                    bluePixels++
                    if (bitmask) {
                        out?.set(w, h, skyColorOverlay)
                    } else {
                        out?.set(w, h, pixel)
                    }
                } else if (isObstacle.isSatisfiedBy(pixel)) {
                    if (bitmask) {
                        out?.set(w, h, excludedColorOverlay)
                    } else {
                        out?.set(w, h, pixel)
                    }
                } else {
                    blueCloudPixels.add(Color.blue(pixel).toFloat())
                    cloudPixels++
                    if (bitmask) {
                        out?.set(w, h, cloudColorOverlay)
                    } else {
                        out?.set(w, h, pixel)
                    }
                }

            }
        }

        val coverage = if (bluePixels + cloudPixels != 0) {
            cloudPixels / (bluePixels + cloudPixels).toFloat()
        } else {
            0f
        }

        val statistics = StatisticsService()

        val blueStandardDeviation = statistics.variance(blueCloudPixels)

        return CloudFeatures(
            coverage,
            blueStandardDeviation / 255
        )
    }


    fun StatisticsService.skewness(values: List<Float>): Float {

        if (values.isEmpty()) {
            return 0f
        }

        val mean = this.mean(values)
        val stdev = this.stdev(values)
        var total = 0.0
        for (value in values) {
            total += power((value - mean).toDouble() / stdev, 3)
        }
        return (total / values.size).toFloat()
    }

}