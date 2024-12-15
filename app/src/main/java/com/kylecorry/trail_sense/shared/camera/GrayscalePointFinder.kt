package com.kylecorry.trail_sense.shared.camera

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.util.Range
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.canvas.PixelCircle
import com.kylecorry.trail_sense.shared.colors.ColorUtils
import com.kylecorry.trail_sense.shared.debugging.isDebug
import kotlin.math.max

class GrayscalePointFinder(
    private val threshold: Float,
    private val minRadius: Float,
    private val aspectRatioRange: Range<Float>
) {

    private val momentFinder = GrayscaleMomentFinder(threshold.toInt(), 1)

    fun getPoints(bitmap: Bitmap): List<PixelCircle> {

        val clusters = mutableListOf<Rect>()

        var x = 0
        while (x < bitmap.width) {
            var y = 0
            while (y < bitmap.height) {
                // Check if the point is already in a cluster
                val hit = clusters.firstOrNull { it.contains(x, y) }
                if (hit != null) {
                    y = hit.bottom + 1
                    continue
                }

                val star = getCluster(x, y, bitmap)
                if (star != null) {
                    clusters.add(star)
                }
                y = star?.bottom ?: y
                y++
            }
            x++
        }

        val filtered = clusters
            .filter {
                val aspectRatio = it.width().toFloat() / it.height().toFloat()
                aspectRatioRange.contains(aspectRatio)
            }
            .map {
                PixelCircle(
                    getCentroid(bitmap, it),
                    max(it.width().toFloat() / 2f, it.height().toFloat() / 2f)
                )
            }.filter {
                it.radius >= minRadius
            }.sortedBy { it.radius }

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

    private fun getCluster(startX: Int, startY: Int, bitmap: Bitmap): Rect? {
        val hits = mutableListOf<PixelCoordinate>()
        val visited = mutableSetOf<PixelCoordinate>()
        val toVisit = mutableListOf(PixelCoordinate(startX.toFloat(), startY.toFloat()))

        while (toVisit.isNotEmpty()) {
            val current = toVisit.removeAt(0)
            if (visited.contains(current)) {
                continue
            }
            visited.add(current)
            val x = current.x.toInt()
            val y = current.y.toInt()
            if (x < 0 || x >= bitmap.width || y < 0 || y >= bitmap.height) {
                continue
            }
            val pixel = bitmap.getPixel(x, y)
            val brightness = ColorUtils.average(pixel)
            if (brightness >= threshold) {
                hits.add(current)
                toVisit.add(PixelCoordinate(x + 1f, y.toFloat()))
                toVisit.add(PixelCoordinate(x - 1f, y.toFloat()))
                toVisit.add(PixelCoordinate(x.toFloat(), y + 1f))
                toVisit.add(PixelCoordinate(x.toFloat(), y - 1f))
            }
        }

        if (hits.isEmpty()) {
            return null
        }

        return Rect(
            hits.minOf { it.x.toInt() },
            hits.minOf { it.y.toInt() },
            hits.maxOf { it.x.toInt() } + 1,
            hits.maxOf { it.y.toInt() } + 1
        )
    }


}