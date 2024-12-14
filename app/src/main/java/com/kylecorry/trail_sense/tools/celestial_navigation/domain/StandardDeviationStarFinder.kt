package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import android.graphics.Bitmap
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.resizeToFit
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath.square
import com.kylecorry.trail_sense.shared.colors.ColorUtils
import kotlin.math.sqrt

class StandardDeviationStarFinder : StarFinder {
    override fun findStars(image: Bitmap): List<PixelCoordinate> {
        val resized = image.resizeToFit(400, 400)

        try {
            var mean = 0.0
            for (x in 0 until resized.width) {
                for (y in 0 until resized.height) {
                    val pixel = resized.getPixel(x, y)
                    val brightness = ColorUtils.average(pixel)
                    mean += brightness
                }
            }

            mean /= resized.width * resized.height

            var stdDev = 0.0
            for (x in 0 until resized.width) {
                for (y in 0 until resized.height) {
                    val pixel = resized.getPixel(x, y)
                    val brightness = ColorUtils.average(pixel)
                    stdDev += square(brightness - mean)
                }
            }

            stdDev = sqrt(stdDev / (resized.width * resized.height))
            resized.recycle()

            val simpleFinder =
                SimpleStarFinder((mean.toFloat() + 5 * stdDev.toFloat()).coerceIn(100f, 240f))
            return simpleFinder.findStars(image)
        } finally {
            resized.recycle()
        }
    }
}