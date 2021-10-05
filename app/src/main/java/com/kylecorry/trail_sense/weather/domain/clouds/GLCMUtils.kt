package com.kylecorry.trail_sense.weather.domain.clouds

import android.graphics.Bitmap
import android.graphics.Color

object GLCMUtils {
    fun Bitmap.glcm(
        step: Pair<Int, Int>,
        channel: ColorChannel,
        excludeTransparent: Boolean = false
    ): Array<FloatArray> {
        // TODO: Make this faster with RenderScript
        val glcm = Array(256) { FloatArray(256) }

        var total = 0

        for (x in 0 until width) {
            for (y in 0 until height) {
                val neighborX = x + step.first
                val neighborY = y + step.second

                if (neighborX >= width || neighborX < 0) {
                    continue
                }

                if (neighborY >= height || neighborY < 0) {
                    continue
                }

                val currentPx = getPixel(x, y)
                val neighborPx = getPixel(neighborX, neighborY)

                if (excludeTransparent && currentPx.getChannel(ColorChannel.Alpha) != 255) {
                    continue
                }

                if (excludeTransparent && neighborPx.getChannel(ColorChannel.Alpha) != 255) {
                    continue
                }

                val current = currentPx.getChannel(channel)
                val neighbor = neighborPx.getChannel(channel)

                glcm[current][neighbor]++
                total++
            }
        }

        if (total > 0) {
            for (row in glcm.indices) {
                for (col in glcm[0].indices) {
                    glcm[row][col] /= total.toFloat()
                }
            }
        }


        return glcm
    }

    private fun Int.getChannel(channel: ColorChannel): Int {
        return when (channel) {
            ColorChannel.Red -> Color.red(this)
            ColorChannel.Green -> Color.green(this)
            ColorChannel.Blue -> Color.blue(this)
            ColorChannel.Alpha -> Color.alpha(this)
        }
    }
}