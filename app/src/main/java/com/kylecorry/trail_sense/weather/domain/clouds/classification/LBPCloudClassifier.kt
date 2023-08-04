package com.kylecorry.trail_sense.weather.domain.clouds.classification

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.core.graphics.blue
import com.kylecorry.sol.math.classifiers.LogisticRegressionClassifier
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.shared.ClassificationResult

class LBPCloudClassifier(
    private val onFeaturesCalculated: (List<Float>) -> Unit = {}
) : ICloudClassifier {

    override suspend fun classify(bitmap: Bitmap): List<ClassificationResult<CloudGenus?>> {

        val regions = mutableListOf<Rect>()
        for (x in 0 until bitmap.width step WINDOW_SIZE) {
            for (y in 0 until bitmap.height step WINDOW_SIZE) {
                regions.add(Rect(x, y, x + WINDOW_SIZE, y + WINDOW_SIZE))
            }
        }

        val histograms = regions.map {
            getLbpHistogram(bitmap, it)
        }

        // Average each value in the histogram
        val lbp = FloatArray(10)
        histograms.forEach {
            it.forEachIndexed { index, value ->
                lbp[index] += value / histograms.size
            }
        }

        val features = lbp.toList()
//        features.add(1f)

        onFeaturesCalculated(features)

//        val isClear = features[0] < 0.55 && features[4] < 0.15f

        val classifier = LogisticRegressionClassifier.fromWeights(weights)

        val prediction = classifier.classify(features)

        val cloudMap = arrayOf(
            CloudGenus.Cirrus,
            CloudGenus.Cirrocumulus,
            CloudGenus.Cirrostratus,
            CloudGenus.Altostratus,
            CloudGenus.Altocumulus,
            CloudGenus.Nimbostratus,
            CloudGenus.Stratocumulus,
            CloudGenus.Cumulus,
            CloudGenus.Stratus,
            CloudGenus.Cumulonimbus
        )

        val result = cloudMap.zip(prediction) { genus, confidence ->
            ClassificationResult<CloudGenus?>(
                genus,
                confidence //* (if (isClear) 0.5f else 1f)
            )
        } + listOf(
            ClassificationResult<CloudGenus?>(
                null,
                0f
//                if (isClear) 0.5f else 0f
            )
        )


        return result.sortedByDescending { it.confidence }
    }

    private fun getLbpHistogram(bitmap: Bitmap, region: Rect? = null): FloatArray {
        val width = bitmap.width
        val height = bitmap.height
        val histogram = FloatArray(10)
        var total = 0f

        val startX = region?.left?.coerceAtLeast(1) ?: 1
        val startY = region?.top?.coerceAtLeast(1) ?: 1
        val endX = region?.right?.coerceAtMost(width - 1) ?: (width - 1)
        val endY = region?.bottom?.coerceAtMost(height - 1) ?: (height - 1)


        for (y in startY until endY) {
            for (x in startX until endX) {
                val lbpValue = getInvariantLbpValue(bitmap, x, y)
                histogram[lbpValue]++
                total++
            }
        }

        for (i in histogram.indices) {
            histogram[i] /= total
        }

        return histogram
    }


    private fun getInvariantLbpValue(bitmap: Bitmap, x: Int, y: Int): Int {
        val centerPixel = bitmap.getPixel(x, y)
        var ones = 0
        var lastBit = false
        var firstBit = false
        var transitions = 0

        val neighbors = arrayOf(
            bitmap.getPixel(x - 1, y - 1), bitmap.getPixel(x, y - 1), bitmap.getPixel(x + 1, y - 1),
            bitmap.getPixel(x + 1, y), bitmap.getPixel(x + 1, y + 1), bitmap.getPixel(x, y + 1),
            bitmap.getPixel(x - 1, y + 1), bitmap.getPixel(x - 1, y)
        )

        for (i in neighbors.indices) {
            val greater = neighbors[i].blue >= centerPixel.blue
            if (greater) {
                ones++
            }
            if (i == 0) {
                firstBit = greater
            } else if (i == neighbors.lastIndex) {
                if (greater != lastBit) transitions++
                if (greater != firstBit) transitions++
            } else if (greater != lastBit) {
                transitions++
            }
            lastBit = greater
        }

        return if (transitions <= 2) ones else 9
    }


    companion object {
        const val IMAGE_SIZE = 200
        private const val WINDOW_SIZE = IMAGE_SIZE / 4
        private val weights = arrayOf(
            arrayOf(-21.966265f,28.689957f,-19.669289f,-6.5124946f,17.906443f,-10.439337f,9.417247f,5.52065f,-11.00236f,8.445709f),
            arrayOf(17.149887f,23.95393f,-25.451288f,-5.401865f,4.8875175f,-24.85154f,8.449413f,0.3145529f,-14.046901f,15.478344f),
            arrayOf(-8.847639f,8.034969f,-19.095787f,-9.402344f,24.446642f,-20.712238f,1.4784786f,28.366844f,-4.6985736f,1.0376198f),
            arrayOf(-40.81389f,-6.41507f,-42.6421f,-7.6938214f,76.28874f,-18.092932f,-8.316817f,49.0562f,-1.3787556f,0.52039146f),
            arrayOf(20.166777f,-18.686024f,5.2460938f,-8.327857f,15.086369f,-22.379744f,18.077255f,19.026236f,-7.6977167f,-20.106577f),
            arrayOf(-0.6161128f,-14.712973f,11.362976f,8.153699f,-31.46621f,11.129078f,24.099335f,-6.4406004f,-4.8199573f,3.8203425f),
            arrayOf(-11.584481f,10.539278f,2.0600166f,14.604518f,-16.457907f,23.488136f,-0.77689004f,-37.78217f,10.11471f,6.294231f),
            arrayOf(-11.205075f,1.2712396f,32.09909f,1.2289957f,-16.521963f,35.78284f,-1.7209977f,-37.612404f,18.647196f,-21.393064f),
            arrayOf(-8.377332f,-34.51078f,-16.04884f,17.032911f,-6.821736f,12.678213f,-9.410478f,29.169428f,-0.6306728f,17.481724f),
            arrayOf(34.72084f,33.94367f,12.911984f,-27.423656f,3.8940024f,-30.207813f,-13.361721f,-2.5591955f,-5.484207f,-6.075042f)
        )
    }
}