package com.kylecorry.trail_sense.weather.domain.clouds.mask

import android.graphics.Bitmap
import com.kylecorry.sol.math.SolMath
import com.kylecorry.trail_sense.shared.colors.ColorUtils
import kotlin.math.roundToInt

class NRBRSkyThresholdCalculator : ISkyThresholdCalculator {

    override suspend fun getThreshold(bitmap: Bitmap): Int {
        var averageNRBR = 0.0

        for (w in 0 until bitmap.width) {
            for (h in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(w, h)
                averageNRBR += nrbr(pixel)
            }
        }

        averageNRBR /= bitmap.width * bitmap.height

        return (averageNRBR * 200).roundToInt().coerceIn(0, 80)
    }

    private fun nrbr(value: Int): Float {
        return SolMath.map(ColorUtils.nrbr(value), -1f, 1f, 0f, 1f)
    }

}