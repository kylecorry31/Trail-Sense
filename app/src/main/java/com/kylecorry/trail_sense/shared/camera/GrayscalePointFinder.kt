package com.kylecorry.trail_sense.shared.camera

import android.graphics.Bitmap
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.canvas.PixelCircle
import com.kylecorry.trail_sense.shared.colors.ColorUtils
import com.kylecorry.trail_sense.shared.extensions.squaredDistanceTo

class GrayscalePointFinder(
    private val threshold: Float,
    private val minRadius: Float,
    maxClusterDistance: Float = 10f
) {

    private val squaredMaxClusterDistance = maxClusterDistance * maxClusterDistance

    fun getPoints(bitmap: Bitmap): List<PixelCircle> {

        val points = mutableListOf<PixelCoordinate>()

        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(x, y)
                val brightness = ColorUtils.average(pixel)
                if (brightness >= threshold) {
                    points.add(PixelCoordinate(x.toFloat(), y.toFloat()))
                }
            }
        }

        // Cluster the points
        val clusters = mutableListOf<Cluster>()

        for (point in points) {
            val cluster = clusters.firstOrNull { it.shouldBelongToCluster(point) }
            if (cluster != null) {
                cluster.add(point)
            } else {
                val newCluster = Cluster(squaredMaxClusterDistance)
                newCluster.add(point)
                clusters.add(newCluster)
            }
        }

        // Calculate the center and radius of each cluster
        return clusters.map { cluster ->
            PixelCircle(cluster.center, cluster.radius)
        }.filter { it.radius > minRadius }.sortedBy { it.radius }

    }

    private class Cluster(private val squaredMaxClusterDistance: Float) {
        var center: PixelCoordinate = PixelCoordinate(0f, 0f)
        val points = mutableListOf<PixelCoordinate>()

        val radius: Float
            get() {
                return points.maxOf { center.squaredDistanceTo(it) }
            }

        fun add(point: PixelCoordinate) {
            points.add(point)
            val x = points.sumOf { it.x.toDouble() } / points.size
            val y = points.sumOf { it.y.toDouble() } / points.size
            center = PixelCoordinate(x.toFloat(), y.toFloat())
        }

        fun shouldBelongToCluster(point: PixelCoordinate): Boolean {
            return center.squaredDistanceTo(point) < squaredMaxClusterDistance
        }
    }

}