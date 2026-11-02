package com.kylecorry.trail_sense.tools.augmented_reality.domain.calibration

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class DiscDetection(
    val x: Double,
    val y: Double,
    val radius: Double,
    val peak: Int,
    val background: Int
)

/**
 * Finds the sun or moon in a camera frame: the brightest compact, round blob near where the
 * sensors predict it to be.
 */
class BrightDiscFinder(
    private val minPeak: Int = 200,
    private val minContrast: Int = 80,
    private val maxRoundnessRatio: Double = 1.6,
    private val minFill: Double = 0.55
) {

    /**
     * @param pixels ARGB pixels, row major
     * @param searchRadius pixels around the predicted location to search
     * @param maxRadius the largest blob radius in pixels to accept (glare makes the sun look bigger)
     */
    fun find(
        pixels: IntArray,
        width: Int,
        height: Int,
        predictedX: Double,
        predictedY: Double,
        searchRadius: Double,
        maxRadius: Double
    ): DiscDetection? {
        val x0 = max(0, (predictedX - searchRadius).toInt())
        val x1 = min(width - 1, (predictedX + searchRadius).toInt())
        val y0 = max(0, (predictedY - searchRadius).toInt())
        val y1 = min(height - 1, (predictedY + searchRadius).toInt())
        if (x0 > x1 || y0 > y1) return null

        var peak = -1
        var peakX = 0
        var peakY = 0
        val step = 2
        val samples = mutableListOf<Int>()
        for (y in y0..y1 step step) {
            for (x in x0..x1 step step) {
                val v = luminance(pixels[y * width + x])
                samples.add(v)
                if (v > peak) {
                    peak = v
                    peakX = x
                    peakY = y
                }
            }
        }
        if (peak < minPeak || samples.isEmpty()) return null
        samples.sort()
        val background = samples[samples.size / 2]
        if (peak - background < minContrast) return null

        // Flood fill the bright core from the peak
        val threshold = background + (peak - background) * 0.75
        val maxArea = (Math.PI * maxRadius * maxRadius * 1.5).toInt().coerceAtLeast(16)
        val visited = HashSet<Int>()
        val queue = ArrayDeque<Int>()
        val start = peakY * width + peakX
        queue.add(start)
        visited.add(start)
        var sumW = 0.0
        var sumX = 0.0
        var sumY = 0.0
        var minX = peakX
        var maxX = peakX
        var minY = peakY
        var maxY = peakY
        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()
            val x = index % width
            val y = index / width
            val w = luminance(pixels[index]) - background.toDouble()
            sumW += w
            sumX += w * (x + 0.5)
            sumY += w * (y + 0.5)
            minX = min(minX, x); maxX = max(maxX, x)
            minY = min(minY, y); maxY = max(maxY, y)
            if (visited.size > maxArea) return null
            for ((dx, dy) in NEIGHBORS) {
                val nx = x + dx
                val ny = y + dy
                if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue
                val n = ny * width + nx
                if (n in visited) continue
                if (luminance(pixels[n]) >= threshold) {
                    visited.add(n)
                    queue.add(n)
                }
            }
        }

        val w = (maxX - minX + 1).toDouble()
        val h = (maxY - minY + 1).toDouble()
        if (max(w, h) / min(w, h) > maxRoundnessRatio) return null
        val radius = sqrt(visited.size / Math.PI)
        // A round blob fills ~78% of its bounding box
        if (visited.size / (w * h) < minFill) return null
        if (radius > maxRadius) return null
        if (sumW <= 0) return null
        val cx = sumX / sumW
        val cy = sumY / sumW
        if (hypot(cx - predictedX, cy - predictedY) > searchRadius) return null
        return DiscDetection(cx, cy, radius, peak, background)
    }

    private fun luminance(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000
    }

    companion object {
        private val NEIGHBORS = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
    }
}
