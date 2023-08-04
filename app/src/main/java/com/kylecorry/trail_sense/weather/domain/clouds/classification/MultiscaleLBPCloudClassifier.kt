package com.kylecorry.trail_sense.weather.domain.clouds.classification

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.core.graphics.red
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.resizeExact
import com.kylecorry.sol.math.classifiers.LogisticRegressionClassifier
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.shared.ClassificationResult

class MultiscaleLBPCloudClassifier(
    private val onFeaturesCalculated: (List<Float>) -> Unit = {}
) : ICloudClassifier {

    override suspend fun classify(bitmap: Bitmap): List<ClassificationResult<CloudGenus?>> {

        // Calculate the LBP for the whole image (smaller scale details)
        val lbpSmallScale = lbp(bitmap, WINDOW_SIZE_SMALL_SCALE)

        // Calculate the LBP for a smaller version of the image (larger scale details)
        val smallerBitmap = bitmap.resizeExact(IMAGE_SIZE_LARGE_SCALE, IMAGE_SIZE_LARGE_SCALE)
        try {
            val lbpLargeScale = lbp(smallerBitmap)

            val features = (lbpSmallScale + lbpLargeScale).toList()

            onFeaturesCalculated(features)

//        val isClear = features[0] < 0.55 && features[4] < 0.15f

            val classifier = LogisticRegressionClassifier.fromWeights(weights)


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

            val prediction = classifier.classify(features)
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
        } finally {
            smallerBitmap.recycle()
        }
    }

    private fun lbp(bitmap: Bitmap, windowSize: Int? = null): FloatArray {
        if (windowSize == null) {
            return getLbpHistogram(bitmap)
        }

        val regions = mutableListOf<Rect>()
        for (x in 0 until bitmap.width step windowSize) {
            for (y in 0 until bitmap.height step windowSize) {
                regions.add(Rect(x, y, x + windowSize, y + windowSize))
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

        return lbp
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

        val center = centerPixel.red

        for (i in neighbors.indices) {
            val greater = neighbors[i].red >= center
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
        const val IMAGE_SIZE = 400
        const val IMAGE_SIZE_LARGE_SCALE = 100
        private const val WINDOW_SIZE_SMALL_SCALE = IMAGE_SIZE / 4
        private const val WINDOW_SIZE_LARGE_SCALE = IMAGE_SIZE_LARGE_SCALE / 4
        private val weights = arrayOf(
            arrayOf(-5.4952374f,14.3737f,-10.882078f,-3.255804f,3.823193f,-2.8884892f,6.668241f,-0.832418f,-1.859536f,0.6951365f),
            arrayOf(1.1457338f,19.609545f,-8.704219f,-0.15394968f,-8.451331f,3.6013277f,-4.1095495f,-3.6958568f,-2.0789752f,3.1820178f),
            arrayOf(6.7047973f,3.0830042f,-8.238715f,-5.5073886f,8.729959f,-6.82094f,4.3253193f,-0.0363455f,-3.7712533f,2.1054692f),
            arrayOf(-18.21645f,-9.126273f,-18.36646f,-6.8289104f,41.734936f,-21.767262f,15.309f,19.13645f,-7.292664f,5.9592824f),
            arrayOf(15.616538f,-8.76335f,-22.957687f,-15.834219f,31.700148f,-44.9996f,39.803314f,7.9940443f,-9.861126f,7.9073844f),
            arrayOf(-11.872581f,9.49199f,2.5856948f,15.971031f,-8.413082f,-1.5221947f,-17.45032f,22.554323f,-8.283221f,-2.5849464f),
            arrayOf(26.504328f,-3.0573368f,27.25383f,8.799754f,-8.971977f,-3.0444381f,-27.23077f,-12.135906f,-3.9422965f,-3.6586668f),
            arrayOf(41.70144f,-32.33795f,40.759766f,-2.87653f,-3.5337694f,-17.273558f,13.698017f,-21.301947f,2.589413f,-20.969849f),
            arrayOf(-12.25585f,1.0795798f,-16.591925f,9.391767f,-9.351604f,33.421925f,-1.2435396f,-19.715761f,-5.0261602f,20.608599f),
            arrayOf(-22.46554f,8.423439f,-7.6144776f,-15.783428f,-0.016059287f,18.595003f,-3.208793f,11.510101f,23.765535f,-12.49839f),
            arrayOf(3.5873046f,16.750248f,-10.0935545f,-3.7497709f,15.082966f,-7.721108f,8.281942f,-17.266544f,-6.892593f,2.5609255f),
            arrayOf(44.351185f,13.153697f,-9.162293f,-4.290022f,-4.680824f,-10.652019f,20.526693f,-43.602493f,-6.1145205f,0.8628841f),
            arrayOf(-0.23207298f,0.50697947f,-6.8701806f,-0.52500284f,5.6606627f,-10.132881f,7.1389914f,3.5591712f,0.5564963f,0.90878475f),
            arrayOf(-27.655094f,-17.566086f,-24.831867f,-0.038645342f,52.580395f,-6.2711396f,-9.969889f,26.273338f,8.081296f,-0.23154052f),
            arrayOf(23.62309f,2.1759636f,14.126063f,3.760014f,-23.27429f,-9.161221f,-3.2834785f,2.0302908f,-7.4835486f,-2.066845f),
            arrayOf(-22.30737f,2.9640496f,1.2861146f,2.8930092f,-25.097383f,18.771051f,23.01847f,-8.652806f,-0.5972308f,8.122886f),
            arrayOf(-20.367853f,-13.729655f,-9.253454f,1.8178215f,17.71808f,11.071396f,18.212843f,-21.645912f,8.914153f,7.718482f),
            arrayOf(-5.6944284f,-23.249775f,1.4978211f,-0.8388633f,13.159605f,3.0490649f,3.0190952f,10.465397f,-0.018448314f,-0.8689759f),
            arrayOf(8.062579f,-18.31595f,4.3917193f,-16.274044f,12.708615f,-18.84219f,-24.521013f,55.92148f,-3.0963252f,0.49164608f),
            arrayOf(17.86629f,40.12858f,15.943505f,0.97814894f,-16.581198f,-12.880777f,-15.731276f,-3.5121963f,-9.039307f,-16.704418f)
        )
    }
}