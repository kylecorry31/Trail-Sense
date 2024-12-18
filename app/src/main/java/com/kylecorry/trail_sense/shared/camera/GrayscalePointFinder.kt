package com.kylecorry.trail_sense.shared.camera

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.util.Range
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.blobs
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.threshold
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.canvas.PixelCircle
import com.kylecorry.trail_sense.shared.debugging.isDebug
import kotlin.math.max

class GrayscalePointFinder(
    private val threshold: Float,
    private val minRadius: Float,
    private val aspectRatioRange: Range<Float>
) {

    private val momentFinder = GrayscaleMomentFinder(0f, 0)

    fun getPoints(bitmap: Bitmap): List<PixelCircle> {
        val clusters = bitmap.blobs(threshold)

        val thresholdImage = bitmap.threshold(threshold, false)
        val filtered = clusters
            .filter {
                val aspectRatio = it.width().toFloat() / it.height().toFloat()
                aspectRatioRange.contains(aspectRatio)
            }
            .map {
                PixelCircle(
                    getCentroid(thresholdImage, it),
                    max(it.width().toFloat() / 2f, it.height().toFloat() / 2f)
                )
            }.filter {
                it.radius >= minRadius
            }.sortedBy { it.radius }

        thresholdImage.recycle()

        if (isDebug()) {
            val debugImage = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            for (cluster in clusters) {
                val aspectRatio = cluster.width().toFloat() / cluster.height().toFloat()
                val radius = max(cluster.width().toFloat() / 2f, cluster.height().toFloat() / 2f)

                val color = if (!aspectRatioRange.contains(aspectRatio)) {
                    Color.GREEN
                } else if (radius < minRadius) {
                    Color.BLUE
                } else {
                    Color.RED
                }

                for (x in cluster.left until cluster.right) {
                    debugImage.setPixel(x, cluster.top, color)
                    debugImage.setPixel(x, cluster.bottom - 1, color)
                }
                for (y in cluster.top until cluster.bottom) {
                    debugImage.setPixel(cluster.left, y, color)
                    debugImage.setPixel(cluster.right - 1, y, color)
                }
            }
            println("Debug image available")
        }

        return filtered
    }

    private fun getCentroid(bitmap: Bitmap, rect: Rect): PixelCoordinate {
        return momentFinder.getMoment(bitmap, rect) ?: PixelCoordinate(
            rect.centerX().toFloat(),
            rect.centerY().toFloat()
        )
    }


}