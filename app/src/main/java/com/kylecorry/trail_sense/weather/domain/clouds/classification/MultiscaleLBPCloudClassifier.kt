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
            val lbpLargeScale = lbp(smallerBitmap, WINDOW_SIZE_LARGE_SCALE)

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
        const val IMAGE_SIZE = 300
        const val IMAGE_SIZE_LARGE_SCALE = 60
        private const val WINDOW_SIZE_SMALL_SCALE = IMAGE_SIZE / 3
        private const val WINDOW_SIZE_LARGE_SCALE = IMAGE_SIZE_LARGE_SCALE / 6
        private val weights = arrayOf(
            arrayOf(-11.068808f,17.765856f,-5.3535295f,0.07620071f,-2.2555523f,-1.724828f,0.13477342f,4.0264635f,-5.112043f,3.8211448f),
            arrayOf(14.255244f,19.029453f,-9.303273f,5.2269077f,-11.489397f,-0.64375705f,-7.708966f,-7.4350343f,-8.60904f,7.1763706f),
            arrayOf(13.396552f,10.416283f,-7.3243346f,-5.7530212f,-4.246768f,-4.8002033f,-1.9275551f,5.5173535f,-5.653589f,0.8973855f),
            arrayOf(-25.420658f,-7.2318916f,-21.49326f,-8.409568f,31.9689f,-15.312918f,-3.0796967f,46.86405f,-6.536049f,9.392743f),
            arrayOf(19.401104f,-7.5386405f,-14.876744f,-22.90962f,33.81736f,-37.385387f,22.365065f,13.5871725f,-9.252246f,3.3320918f),
            arrayOf(-12.920328f,-3.0790293f,-7.3546653f,5.127237f,-10.610591f,-7.035677f,23.650303f,5.204639f,-3.3687859f,10.801804f),
            arrayOf(32.288597f,-5.270817f,21.237213f,-0.106278844f,-11.078324f,11.776494f,2.2051065f,-51.954918f,6.4286084f,-5.142321f),
            arrayOf(14.250272f,-25.64478f,30.466854f,-7.9588747f,-15.545681f,23.756657f,25.688108f,-36.6392f,24.518145f,-32.262356f),
            arrayOf(-17.377268f,-4.8392725f,-9.703994f,12.574182f,9.500917f,5.6816554f,-23.036018f,15.916069f,-7.4950852f,19.257702f),
            arrayOf(-4.6834607f,21.31112f,8.834105f,1.9904815f,-23.342178f,15.940151f,-16.654587f,-1.6933304f,15.644624f,-16.863276f),
            arrayOf(-5.125682f,22.013353f,-8.600981f,-8.512174f,16.345959f,-9.61421f,15.103185f,-23.51155f,-6.0187044f,8.506746f),
            arrayOf(37.106422f,4.8371477f,4.33674f,-10.173914f,-12.99431f,-8.126305f,14.839761f,-21.579138f,-8.049368f,0.36887455f),
            arrayOf(-11.558327f,-0.5348042f,-2.9828174f,-5.4680357f,-18.115116f,12.277366f,11.073502f,11.43265f,3.7492347f,0.6095583f),
            arrayOf(-45.614307f,-3.5914805f,-4.28038f,21.411486f,44.75272f,16.356583f,-43.23258f,10.004031f,13.123517f,-8.527475f),
            arrayOf(17.86295f,3.5921075f,24.748598f,10.066247f,-30.714691f,10.379776f,-23.535166f,3.7331161f,-9.199979f,-6.4944415f),
            arrayOf(-11.283743f,-3.9938242f,-17.011934f,-6.249236f,-11.916189f,-7.0555935f,50.50706f,5.216298f,-10.732316f,12.983255f),
            arrayOf(-3.193988f,-0.3468636f,-14.235194f,-2.5788736f,-15.078647f,4.4404964f,18.784832f,-30.266226f,30.303478f,12.709019f),
            arrayOf(31.814934f,-31.543919f,-16.22511f,7.2474155f,9.290913f,-12.330596f,4.9953465f,-4.6967764f,12.640059f,-0.6377827f),
            arrayOf(13.302249f,-2.3483632f,9.268448f,-17.544508f,-11.264643f,6.9889827f,-7.996197f,27.683092f,-4.315705f,-13.31154f),
            arrayOf(-1.1965523f,26.799494f,10.172923f,-8.414442f,26.376186f,-23.090612f,-18.93402f,15.501181f,-20.99771f,-5.7648864f)
        )
    }
}