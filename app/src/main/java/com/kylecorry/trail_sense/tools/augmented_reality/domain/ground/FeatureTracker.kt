package com.kylecorry.trail_sense.tools.augmented_reality.domain.ground

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Finds corners and follows them from frame to frame with pyramidal Lucas-Kanade optical flow.
 * Each feature keeps an id so it can be followed across many frames.
 */
class FeatureTracker {

    class Feature(val id: Long, val x: Float, val y: Float)

    class Image(val pixels: FloatArray, val width: Int, val height: Int) {
        operator fun get(x: Int, y: Int) = pixels[y * width + x]

        fun sample(x: Float, y: Float): Float {
            val x0 = floor(x).toInt().coerceIn(0, width - 2)
            val y0 = floor(y).toInt().coerceIn(0, height - 2)
            val fx = (x - x0).coerceIn(0f, 1f)
            val fy = (y - y0).coerceIn(0f, 1f)
            val i = y0 * width + x0
            val top = pixels[i] * (1 - fx) + pixels[i + 1] * fx
            val bottom = pixels[i + width] * (1 - fx) + pixels[i + width + 1] * fx
            return top * (1 - fy) + bottom * fy
        }

        fun contains(x: Float, y: Float, margin: Float): Boolean {
            return isWithin(x, width, margin) && isWithin(y, height, margin)
        }

        private fun isWithin(value: Float, size: Int, margin: Float): Boolean {
            return value >= margin && value < size - margin
        }

        fun half(): Image {
            val w = width / 2
            val h = height / 2
            val out = FloatArray(w * h)
            for (y in 0 until h) for (x in 0 until w) {
                val i = (2 * y) * width + 2 * x
                out[y * w + x] = (pixels[i] + pixels[i + 1] + pixels[i + width] + pixels[i + width + 1]) / 4f
            }
            return Image(out, w, h)
        }
    }

    private var previous: List<Image>? = null
    private var features = listOf<Feature>()
    private var nextId = 0L

    /** Tracks the current features into the new frame and tops them up. Returns the features in it. */
    fun track(image: Image): List<Feature> {
        val pyramid = pyramid(image)
        val last = previous
        val tracked = if (last == null) emptyList() else features.mapNotNull { f ->
            val forward = flow(last, pyramid, f.x, f.y) ?: return@mapNotNull null
            // Forward-backward check rejects features that slid or got occluded
            val backward = flow(pyramid, last, forward.first, forward.second) ?: return@mapNotNull null
            val error = abs(backward.first - f.x) + abs(backward.second - f.y)
            if (error > MAX_FORWARD_BACKWARD) return@mapNotNull null
            Feature(f.id, forward.first, forward.second)
        }
        features = tracked + detect(image, tracked)
        previous = pyramid
        return features
    }

    fun reset() {
        previous = null
        features = emptyList()
    }

    private fun pyramid(image: Image): List<Image> {
        val levels = mutableListOf(image)
        repeat(LEVELS - 1) { levels.add(levels.last().half()) }
        return levels
    }

    // Shi-Tomasi corners, the best one in each grid cell that has no feature yet
    private fun detect(image: Image, existing: List<Feature>): List<Feature> {
        val cellW = image.width / GRID_X
        val cellH = image.height / GRID_Y
        val occupied = existing.map {
            (it.y / cellH).toInt().coerceIn(0, GRID_Y - 1) * GRID_X + (it.x / cellW).toInt().coerceIn(0, GRID_X - 1)
        }.toSet()
        val margin = WINDOW + 2
        return (0 until GRID_X * GRID_Y).filter { it !in occupied }.mapNotNull { cell ->
            val cx = cell % GRID_X
            val cy = cell / GRID_X
            bestCorner(
                image,
                (cx * cellW).coerceAtLeast(margin),
                ((cx + 1) * cellW).coerceAtMost(image.width - margin),
                (cy * cellH).coerceAtLeast(margin),
                ((cy + 1) * cellH).coerceAtMost(image.height - margin)
            )?.let { Feature(nextId++, it.first.toFloat(), it.second.toFloat()) }
        }
    }

    private fun bestCorner(image: Image, x0: Int, x1: Int, y0: Int, y1: Int): Pair<Int, Int>? {
        var best = MIN_CORNER
        var corner: Pair<Int, Int>? = null
        for (y in y0 until y1 step 2) for (x in x0 until x1 step 2) {
            val score = cornerScore(image, x, y)
            if (score > best) {
                best = score
                corner = x to y
            }
        }
        return corner
    }

    // Smallest eigenvalue of the structure tensor
    private fun cornerScore(image: Image, x: Int, y: Int): Float {
        var gxx = 0f
        var gxy = 0f
        var gyy = 0f
        for (dy in -2..2) for (dx in -2..2) {
            val gx = (image[x + dx + 1, y + dy] - image[x + dx - 1, y + dy]) / 2f
            val gy = (image[x + dx, y + dy + 1] - image[x + dx, y + dy - 1]) / 2f
            gxx += gx * gx
            gxy += gx * gy
            gyy += gy * gy
        }
        val trace = gxx + gyy
        val det = gxx * gyy - gxy * gxy
        return (trace / 2f - sqrt((trace * trace / 4f - det).coerceAtLeast(0f))) / 25f
    }

    private fun flow(from: List<Image>, to: List<Image>, x: Float, y: Float): Pair<Float, Float>? {
        var gx = 0f
        var gy = 0f
        for (level in LEVELS - 1 downTo 0) {
            val scale = 1f / (1 shl level)
            val px = x * scale
            val py = y * scale
            if (from[level].contains(px, py, WINDOW + 2f)) {
                val (vx, vy) = levelFlow(from[level], to[level], px, py, gx, gy) ?: return null
                gx += vx
                gy += vy
            } else if (level == 0) {
                return null
            }
            if (level > 0) {
                gx *= 2f
                gy *= 2f
            }
        }
        val nx = x + gx
        val ny = y + gy
        return if (to[0].contains(nx, ny, WINDOW.toFloat())) nx to ny else null
    }

    // Lucas-Kanade at one pyramid level, starting from a guess of the displacement
    private fun levelFlow(a: Image, b: Image, px: Float, py: Float, gx: Float, gy: Float): Pair<Float, Float>? {
        var gxx = 0f
        var gxy = 0f
        var gyy = 0f
        val n = (2 * WINDOW + 1) * (2 * WINDOW + 1)
        val ix = FloatArray(n)
        val iy = FloatArray(n)
        val template = FloatArray(n)
        var k = 0
        for (dy in -WINDOW..WINDOW) for (dx in -WINDOW..WINDOW) {
            val sx = px + dx
            val sy = py + dy
            val vx = (a.sample(sx + 1, sy) - a.sample(sx - 1, sy)) / 2f
            val vy = (a.sample(sx, sy + 1) - a.sample(sx, sy - 1)) / 2f
            ix[k] = vx
            iy[k] = vy
            template[k] = a.sample(sx, sy)
            gxx += vx * vx
            gxy += vx * vy
            gyy += vy * vy
            k++
        }
        val det = gxx * gyy - gxy * gxy
        if (det < MIN_DETERMINANT) return null
        var vx = 0f
        var vy = 0f
        repeat(ITERATIONS) {
            var bx = 0f
            var by = 0f
            k = 0
            for (dy in -WINDOW..WINDOW) for (dx in -WINDOW..WINDOW) {
                val diff = template[k] - b.sample(px + gx + vx + dx, py + gy + vy + dy)
                bx += diff * ix[k]
                by += diff * iy[k]
                k++
            }
            val ux = (gyy * bx - gxy * by) / det
            val uy = (gxx * by - gxy * bx) / det
            vx += ux
            vy += uy
            if (abs(ux) + abs(uy) < CONVERGED) return vx to vy
        }
        return vx to vy
    }

    private companion object {
        const val LEVELS = 3
        const val WINDOW = 4
        const val ITERATIONS = 10
        const val GRID_X = 8
        const val GRID_Y = 10
        const val MIN_CORNER = 2f
        const val MAX_FORWARD_BACKWARD = 1f
        const val MIN_DETERMINANT = 1e-3f
        const val CONVERGED = 0.01f
    }
}
