package com.kylecorry.trail_sense.weather.domain.clouds

import android.graphics.Bitmap
import android.graphics.Color
import com.kylecorry.sol.math.SolMath
import kotlin.math.roundToInt

class NRBRSkyThresholdCalculator : ISkyThresholdCalculator {

    override suspend fun getThreshold(bitmap: Bitmap): Int {
        var averageNRBR = 0.0

//        val nrbrHistogram = MutableList(101){ 0 }
        for (w in 0 until bitmap.width) {
            for (h in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(w, h)
//                nrbrHistogram[nrbr(pixel)]++
                averageNRBR += nrbr(pixel)
            }
        }

        averageNRBR /= bitmap.width * bitmap.height

        //        println(nrbrHistogram)

        return (averageNRBR * 200).roundToInt().coerceIn(0, 80)
    }

    private fun nrbr(value: Int): Float {
        val blue = Color.blue(value)
        val red = Color.red(value)

        val nrbr = (red - blue) / (red + blue).toFloat().coerceAtLeast(1f)

        return SolMath.map(nrbr, -1f, 1f, 0f, 1f)
    }

}