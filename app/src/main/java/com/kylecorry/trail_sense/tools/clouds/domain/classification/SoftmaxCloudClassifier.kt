package com.kylecorry.trail_sense.tools.clouds.domain.classification

import android.graphics.Bitmap
import android.graphics.Color
import com.kylecorry.sol.math.algebra.Matrix
import com.kylecorry.sol.math.classifiers.LogisticRegressionClassifier
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class SoftmaxCloudClassifier(
    private val onFeaturesCalculated: (List<Float>) -> Unit = {}
) : ICloudClassifier {

    override suspend fun classify(bitmap: Bitmap): List<ClassificationResult<CloudGenus?>> {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val features = colorFeatures(pixels) + textureFeatures(pixels, bitmap.width, bitmap.height)
        onFeaturesCalculated(features)

        val prediction = LogisticRegressionClassifier.fromWeights(weights).classify(features + 1f)
        return classes.zip(prediction) { genus, confidence -> ClassificationResult(genus, confidence) }
            .sortedByDescending { it.confidence }
    }

    private fun colorFeatures(pixels: IntArray): List<Float> {
        var sum = 0.0
        var sumOfSquares = 0.0
        for (pixel in pixels) {
            val red = srgbToLinear[Color.red(pixel)]
            val blue = srgbToLinear[Color.blue(pixel)]
            val nrbr = (red - blue) / max(red + blue, 0.001)
            sum += nrbr
            sumOfSquares += nrbr * nrbr
        }
        val mean = sum / pixels.size
        val variance = max(sumOfSquares / pixels.size - mean * mean, 0.0)
        return listOf(mean.toFloat(), sqrt(variance).toFloat())
    }

    private fun textureFeatures(pixels: IntArray, width: Int, height: Int): List<Float> {
        val blockSize = minOf(width, height) / TEXTURE_SIZE
        require(blockSize >= 1) { "Image must be at least $TEXTURE_SIZE pixels on each side" }

        val brightness = IntArray(TEXTURE_SIZE * TEXTURE_SIZE)
        for (y in 0 until TEXTURE_SIZE * blockSize) {
            for (x in 0 until TEXTURE_SIZE * blockSize) {
                val pixel = pixels[y * width + x]
                brightness[(y / blockSize) * TEXTURE_SIZE + x / blockSize] +=
                    Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)
            }
        }

        val threshold = LBP_THRESHOLD * 3 * blockSize * blockSize
        return LBP_RADII.flatMap { localBinaryPatternShapes(brightness, it, threshold) }
    }

    /**
     * @return the fraction of pixels whose ring of 8 neighbors at [radius] is flat, a single edge, or irregular.
     * A neighbor is "on" when it is at least [threshold] brighter than the center.
     */
    private fun localBinaryPatternShapes(brightness: IntArray, radius: Int, threshold: Int): List<Float> {
        val offsets = listOf(0 to 1, -1 to 1, -1 to 0, -1 to -1, 0 to -1, 1 to -1, 1 to 0, 1 to 1)
            .map { (dy, dx) -> radius * (dy * TEXTURE_SIZE + dx) }
        val isOn = BooleanArray(8)
        var flat = 0
        var edge = 0
        var irregular = 0
        for (y in radius until TEXTURE_SIZE - radius) {
            for (x in radius until TEXTURE_SIZE - radius) {
                val center = y * TEXTURE_SIZE + x
                var onCount = 0
                for (i in 0 until 8) {
                    isOn[i] = brightness[center + offsets[i]] >= brightness[center] + threshold
                    if (isOn[i]) onCount++
                }
                var transitions = 0
                for (i in 0 until 8) {
                    if (isOn[i] != isOn[(i + 7) % 8]) transitions++
                }
                when {
                    transitions > 2 -> irregular++
                    onCount == 0 || onCount == 8 -> flat++
                    onCount in 3..5 -> edge++
                }
            }
        }
        val total = (TEXTURE_SIZE - 2 * radius).toFloat().pow(2)
        return listOf(flat / total, edge / total, irregular / total)
    }

    companion object {
        const val IMAGE_SIZE = 400
        private const val TEXTURE_SIZE = 200
        private const val LBP_THRESHOLD = 3
        private val LBP_RADII = listOf(1, 2, 4)

        private val srgbToLinear = DoubleArray(256) {
            val value = it / 255.0
            if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }

        private val classes = listOf(
            CloudGenus.Cirrus,
            CloudGenus.Cirrocumulus,
            CloudGenus.Cirrostratus,
            CloudGenus.Altostratus,
            CloudGenus.Altocumulus,
            CloudGenus.Nimbostratus,
            CloudGenus.Stratocumulus,
            CloudGenus.Cumulus,
            CloudGenus.Stratus,
            CloudGenus.Cumulonimbus,
            null
        )

        private val weights = Matrix.create(
            arrayOf(
                floatArrayOf(-14.5887f, -7.89426f, 0.798298f, 5.14896f, -0.694312f, 7.78797f, 7.5909f, -0.510645f, 6.8277f, 4.95638f, -9.42229f),
                floatArrayOf(-18.9102f, -16.7021f, 9.23345f, 4.44447f, -7.05674f, -5.40085f, -3.70582f, 16.973f, -6.63056f, 25.7675f, 1.98776f),
                floatArrayOf(1.06043f, -2.46502f, 2.26651f, 1.44748f, -6.04572f, 3.27512f, 0.0415452f, -2.71309f, 0.237694f, 0.427236f, 2.46782f),
                floatArrayOf(-0.784907f, -6.83036f, -6.15216f, -0.382464f, -2.29202f, -4.26435f, 15.1142f, 9.87469f, -1.05807f, -0.202175f, -3.02237f),
                floatArrayOf(-12.2924f, 53.352f, -18.6288f, -3.10906f, 1.02841f, 0.799693f, 12.7363f, -12.7421f, -3.66115f, -9.39877f, -8.08416f),
                floatArrayOf(-2.12811f, -3.18771f, 0.203603f, 3.24323f, -7.55161f, 3.99665f, 0.826324f, 0.317681f, -0.442452f, 0.555154f, 4.16725f),
                floatArrayOf(0.768057f, -7.71933f, 1.16595f, -2.75214f, 15.068f, -8.61928f, 4.44574f, 4.10086f, -2.09449f, -0.575713f, -3.78769f),
                floatArrayOf(23.5459f, 13.583f, -5.04473f, -6.03608f, 1.77165f, -7.97218f, -6.03717f, -3.04357f, 0.703347f, -1.82895f, -9.64116f),
                floatArrayOf(-1.83167f, -4.70993f, 2.43332f, -3.9023f, 2.24147f, 1.23299f, -1.1677f, 5.20303f, -3.73208f, 2.26881f, 1.96406f),
                floatArrayOf(6.8365f, 7.47138f, 4.07353f, 0.168514f, -20.4578f, 5.75085f, 6.79644f, 1.53068f, -8.6869f, -3.06765f, -0.41553f),
                floatArrayOf(19.2029f, -1.40591f, 5.46827f, -9.88856f, 10.7015f, -17.91f, 0.771518f, 3.71711f, 3.34855f, 1.64885f, -15.6543f),
                floatArrayOf(-5.85898f, 2.31017f, -1.5106f, 0.286839f, 11.0509f, -1.14557f, 0.9983f, -1.55195f, 6.23965f, -1.59001f, -9.22875f)
            )
        )
    }
}
