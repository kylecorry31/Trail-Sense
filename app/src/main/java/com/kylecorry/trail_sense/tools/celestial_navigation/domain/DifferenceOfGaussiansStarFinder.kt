package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import android.graphics.Bitmap
import android.graphics.Color
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.blur
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.resizeToFit
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.colors.ColorUtils
import kotlin.math.absoluteValue

class DifferenceOfGaussiansStarFinder(
    private val percent: Float = 0.3f,
    private val firstBlur: Int = 1,
    private val secondBlur: Int = 4
) : StarFinder {
    override fun findStars(image: Bitmap): List<PixelCoordinate> {
        val resized = image.resizeToFit(600, 600)
        val blurred1 = resized.blur(firstBlur)
        val blurred2 = resized.blur(secondBlur)
        resized.recycle()

        try {
            val diff = Bitmap.createBitmap(blurred1.width, blurred1.height, Bitmap.Config.ARGB_8888)
            var maxDiff = 0
            for (x in 0 until blurred1.width) {
                for (y in 0 until blurred1.height) {
                    val pixel1 = ColorUtils.average(blurred1.getPixel(x, y))
                    val pixel2 = ColorUtils.average(blurred2.getPixel(x, y))
                    val diffValue = (pixel1 - pixel2).toInt().absoluteValue//.coerceAtLeast(0)
                    if (diffValue > maxDiff) {
                        maxDiff = diffValue
                    }
                    diff.setPixel(x, y, Color.rgb(diffValue, diffValue, diffValue))
                }
            }

            blurred1.recycle()
            blurred2.recycle()

            val xScale = diff.width.toFloat() / image.width
            val yScale = diff.height.toFloat() / image.height
            val simpleFinder = SimpleStarFinder(percent * maxDiff)
            return simpleFinder.findStars(diff).map {
                PixelCoordinate(it.x / xScale, it.y / yScale)
            }
        } finally {
            blurred1.recycle()
            blurred2.recycle()
        }
    }
}