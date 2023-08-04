package com.kylecorry.trail_sense.weather.domain.clouds.classification

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.core.graphics.blue
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.resizeExact
import com.kylecorry.sol.math.classifiers.LogisticRegressionClassifier
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.shared.ClassificationResult

class MultiscaleLBPCloudClassifier(
    private val onFeaturesCalculated: (List<Float>) -> Unit = {}
) : ICloudClassifier {

    override suspend fun classify(bitmap: Bitmap): List<ClassificationResult<CloudGenus?>> {

        // Calculate the LBP for the whole image (smaller scale details)
        val lbpSmallScale = lbp(bitmap)

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
        const val IMAGE_SIZE_LARGE_SCALE = 50
        private const val WINDOW_SIZE = IMAGE_SIZE / 4
        private val weights = arrayOf(
            arrayOf(-15.815831f,24.887293f,-11.415347f,-2.8483725f,-2.758884f,-5.3232846f,6.196435f,12.247065f,-8.573067f,3.8981464f),
            arrayOf(20.593073f,21.165854f,-13.565842f,-0.39457843f,-22.077028f,-15.6509905f,2.1453402f,10.380674f,-10.424429f,8.285748f),
            arrayOf(-2.7012448f,13.348541f,-5.879979f,-5.9758706f,-3.0872102f,-14.84059f,-8.022459f,33.623375f,-3.3261595f,-2.7044618f),
            arrayOf(-29.800743f,-0.64227337f,-28.040405f,-5.9800735f,44.34451f,-11.591286f,-20.199593f,53.540894f,0.09350765f,-1.2354374f),
            arrayOf(15.036339f,-28.555065f,1.6483576f,-10.89382f,26.604855f,-20.234102f,25.183537f,11.766836f,-5.2051997f,-14.84925f),
            arrayOf(1.1995833f,-23.520098f,-1.0446297f,-0.18981083f,-25.172209f,19.137117f,38.32485f,-13.622707f,-2.1960783f,7.6752143f),
            arrayOf(-3.4324083f,2.4832034f,2.7507334f,8.02606f,-15.41715f,24.764479f,-0.05873309f,-37.123043f,9.119796f,9.330786f),
            arrayOf(-8.262626f,-9.269446f,24.798603f,-3.4302838f,6.2609534f,33.006416f,6.0019317f,-52.88873f,16.757841f,-12.50097f),
            arrayOf(-19.81485f,-31.413317f,-15.833538f,17.453272f,0.8301497f,11.995956f,-15.642596f,28.220066f,1.492784f,23.146185f),
            arrayOf(36.65814f,32.25505f,19.187054f,-14.21923f,-8.445981f,-25.02206f,-11.171067f,-7.7364893f,-2.1166818f,-18.939837f),
            arrayOf(-4.511281f,25.188766f,-21.249708f,-9.1658945f,12.039095f,-11.84393f,24.257708f,-2.638903f,-16.635471f,5.056303f),
            arrayOf(28.009045f,0.20875515f,-27.815815f,-11.610025f,0.05020634f,-9.607018f,17.922588f,2.0391564f,-4.6808796f,6.1149874f),
            arrayOf(-26.571978f,-9.89489f,7.325901f,-5.208455f,0.2510296f,13.976632f,-18.356632f,29.362024f,8.05578f,1.5352212f),
            arrayOf(-35.153637f,-8.780423f,9.34355f,26.0229f,3.110858f,4.3134856f,-8.186349f,-2.3054793f,19.979935f,-7.879916f),
            arrayOf(3.3143911f,17.645395f,10.428949f,10.255166f,-12.11284f,2.999639f,-18.500767f,9.409654f,-7.092222f,-15.815871f),
            arrayOf(11.99298f,13.63101f,-6.5963793f,-12.469079f,3.9475944f,-19.231966f,13.10523f,13.187175f,-11.487626f,-5.5080247f),
            arrayOf(15.4136095f,-16.03519f,-18.780334f,4.5301895f,-17.241314f,21.439077f,26.946087f,-31.371698f,13.26193f,2.237123f),
            arrayOf(-18.583218f,-8.636608f,23.59201f,5.7386575f,-8.314778f,-12.70589f,10.034946f,-4.535551f,14.193443f,-0.06965686f),
            arrayOf(23.902792f,-33.570774f,5.573054f,-8.207351f,-9.489728f,24.536657f,-11.723551f,19.99193f,-9.948358f,-0.5317133f),
            arrayOf(-3.7794645f,20.860853f,-8.944297f,-18.443153f,28.855919f,-17.67554f,-12.707789f,5.291226f,-9.91295f,17.020943f)
        )
    }
}