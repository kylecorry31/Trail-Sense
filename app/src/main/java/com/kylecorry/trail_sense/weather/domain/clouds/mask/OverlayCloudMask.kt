package com.kylecorry.trail_sense.weather.domain.clouds.mask

import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.graphics.set
import com.kylecorry.trail_sense.shared.colors.AppColor

class OverlayCloudMask(
    private val pixelClassifier: ICloudPixelClassifier,
    @ColorInt private val cloudColor: Int = Color.WHITE,
    @ColorInt private val skyColor: Int = AppColor.Blue.color,
    @ColorInt private val obstacleColor: Int = AppColor.Red.color
) : ICloudMask {

    override fun mask(input: Bitmap, output: Bitmap?): Bitmap {
        val out = output ?: input.copy(input.config, true)
        for (x in 0 until input.width) {
            for (y in 0 until input.height) {
                val pixel = input.getPixel(x, y)
                when (pixelClassifier.classify(pixel)) {
                    SkyPixelClassification.Sky -> out[x, y] = skyColor
                    SkyPixelClassification.Cloud -> out[x, y] = cloudColor
                    SkyPixelClassification.Obstacle -> out[x, y] = obstacleColor
                }
            }
        }
        return out
    }
}