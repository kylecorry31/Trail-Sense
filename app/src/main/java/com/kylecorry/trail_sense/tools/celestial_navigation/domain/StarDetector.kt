package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import com.kylecorry.sol.science.astronomy.navigation.ImageStar
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A grayscale image stored row major
 */
class LuminanceImage(val width: Int, val height: Int, val data: FloatArray) {
    operator fun get(x: Int, y: Int) = data[y * width + x]

    fun binned(factor: Int): LuminanceImage {
        if (factor <= 1) return this
        val w = width / factor
        val h = height / factor
        val out = FloatArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var sum = 0f
                for (dy in 0 until factor) for (dx in 0 until factor) {
                    sum += data[(y * factor + dy) * width + x * factor + dx]
                }
                out[y * w + x] = sum
            }
        }
        return LuminanceImage(w, h, out)
    }
}

data class DetectorSettings(
    val sigma: Double = 6.0,
    val maxStars: Int = 40,
    val backgroundRadius: Int = 24,
    val peakRadius: Int = 4,
    val centroidRadius: Int = 3,
    val maxBlobPixels: Int = 80,
    val minBlobPixels: Int = 2
)

/**
 * Background subtracted local maxima with intensity weighted centroids
 */
class StarDetector(private val settings: DetectorSettings = DetectorSettings()) {

    class Prepared(val image: LuminanceImage, val residual: FloatArray, val noise: Float, val localNoise: FloatArray)

    fun prepare(image: LuminanceImage): Prepared {
        val w = image.width
        val h = image.height
        val r = settings.backgroundRadius
        val means = boxMean(w, h, r) { image.data[it].toDouble() }
        val residual = FloatArray(w * h) { image.data[it] - means[it] }
        // Robust noise estimate from a sample of the residual
        val step = max(1, residual.size / 200_000)
        val sample = FloatArray(residual.size / step) { abs(residual[it * step]) }
        sample.sort()
        val noise = max(1e-3f, sample[sample.size / 2] * 1.4826f)

        // Local noise (glow from lights and the moon is noisier than the rest of the sky)
        val variance = boxMean(w, h, r) { residual[it].toDouble() * residual[it] }
        for (i in variance.indices) {
            variance[i] = max(noise, kotlin.math.sqrt(variance[i]))
        }
        return Prepared(image, residual, noise, variance)
    }

    private fun boxMean(w: Int, h: Int, r: Int, value: (Int) -> Double): FloatArray {
        val integral = DoubleArray((w + 1) * (h + 1))
        for (y in 0 until h) {
            var row = 0.0
            for (x in 0 until w) {
                row += value(y * w + x)
                integral[(y + 1) * (w + 1) + x + 1] = integral[y * (w + 1) + x + 1] + row
            }
        }
        val out = FloatArray(w * h)
        for (y in 0 until h) {
            val y0 = max(0, y - r)
            val y1 = min(h, y + r + 1)
            for (x in 0 until w) {
                val x0 = max(0, x - r)
                val x1 = min(w, x + r + 1)
                val sum = integral[y1 * (w + 1) + x1] - integral[y0 * (w + 1) + x1] -
                        integral[y1 * (w + 1) + x0] + integral[y0 * (w + 1) + x0]
                out[y * w + x] = (sum / ((x1 - x0) * (y1 - y0))).toFloat()
            }
        }
        return out
    }

    fun detect(prepared: Prepared): List<ImageStar> {
        val w = prepared.image.width
        val h = prepared.image.height
        val residual = prepared.residual
        val pr = settings.peakRadius
        val candidates = mutableListOf<ImageStar>()
        for (y in pr until h - pr) {
            for (x in pr until w - pr) {
                val v = residual[y * w + x]
                val threshold = (settings.sigma * prepared.localNoise[y * w + x]).toFloat()
                if (v < threshold) continue
                var isPeak = true
                loop@ for (dy in -pr..pr) for (dx in -pr..pr) {
                    if (dx == 0 && dy == 0) continue
                    val n = residual[(y + dy) * w + x + dx]
                    if (n > v || (n == v && (dy < 0 || (dy == 0 && dx < 0)))) {
                        isPeak = false
                        break@loop
                    }
                }
                if (!isPeak) continue
                val star = centroid(prepared, x, y, threshold) ?: continue
                candidates.add(star)
            }
        }
        return candidates.sortedByDescending { it.brightness }.take(settings.maxStars)
    }

    /**
     * Finds the brightest point near the given pixel and returns its centroid
     */
    fun refine(prepared: Prepared, x: Double, y: Double, radius: Int): ImageStar? {
        val w = prepared.image.width
        val h = prepared.image.height
        val cx = x.roundToInt()
        val cy = y.roundToInt()
        var bestX = -1
        var bestY = -1
        var best = -Float.MAX_VALUE
        for (py in max(0, cy - radius) until min(h, cy + radius + 1)) {
            for (px in max(0, cx - radius) until min(w, cx + radius + 1)) {
                val v = prepared.residual[py * w + px]
                if (v > best) {
                    best = v
                    bestX = px
                    bestY = py
                }
            }
        }
        if (bestX < 0) return null
        return centroid(prepared, bestX, bestY, (best * 0.2f).coerceAtLeast(prepared.noise), validate = false)
    }

    private fun centroid(prepared: Prepared, x: Int, y: Int, threshold: Float, validate: Boolean = true): ImageStar? {
        val w = prepared.image.width
        val h = prepared.image.height
        val cr = settings.centroidRadius
        var sum = 0.0
        var sx = 0.0
        var sy = 0.0
        var count = 0
        val half = prepared.residual[y * w + x] * 0.5f
        for (dy in -cr..cr) for (dx in -cr..cr) {
            val px = x + dx
            val py = y + dy
            if (px < 0 || py < 0 || px >= w || py >= h) continue
            val v = prepared.residual[py * w + px]
            if (v <= 0) continue
            sum += v
            sx += v * px
            sy += v * py
            if (v >= half) count++
        }
        if (sum <= 0) return null
        if (validate) {
            // Hot pixels are a single pixel, the moon and lights are huge
            if (count < settings.minBlobPixels) return null
            if (blobSize(prepared, x, y, threshold) > settings.maxBlobPixels) return null
        }
        // Pixel centers are at +0.5
        return ImageStar(sx / sum + 0.5, sy / sum + 0.5, sum)
    }

    private fun blobSize(prepared: Prepared, x: Int, y: Int, threshold: Float): Int {
        val w = prepared.image.width
        val h = prepared.image.height
        val r = 8
        var count = 0
        for (py in max(0, y - r) until min(h, y + r + 1)) {
            for (px in max(0, x - r) until min(w, x + r + 1)) {
                if (prepared.residual[py * w + px] >= threshold) count++
            }
        }
        return count
    }
}
