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
            arrayOf(-5.605051f,14.500404f,-10.930604f,-3.2927907f,3.9403503f,-2.9330378f,6.679798f,-0.7713119f,-1.8163729f,0.6855484f),
            arrayOf(1.1431322f,19.733505f,-8.686849f,-0.08633135f,-8.294478f,3.55613f,-4.1757894f,-3.6981966f,-1.9813626f,3.0886028f),
            arrayOf(6.684596f,3.0763505f,-8.275269f,-5.5122533f,8.682882f,-6.84984f,4.320389f,0.069174126f,-3.7446845f,2.097361f),
            arrayOf(-18.160738f,-9.04838f,-18.37681f,-6.796326f,41.69116f,-21.785183f,15.25978f,19.146797f,-7.310033f,5.940853f),
            arrayOf(15.526745f,-8.763447f,-22.88206f,-15.793932f,31.43502f,-45.022385f,39.875412f,7.994009f,-9.935776f,8.0603075f),
            arrayOf(-11.727435f,9.387513f,2.555124f,15.972872f,-8.184564f,-1.5680214f,-17.456396f,22.332245f,-8.262995f,-2.5264103f),
            arrayOf(26.627213f,-2.9506698f,27.323406f,8.722929f,-9.107892f,-2.9829404f,-27.345907f,-12.197027f,-3.950217f,-3.6703482f),
            arrayOf(41.839104f,-32.33254f,40.737877f,-2.8424535f,-3.7936003f,-17.301603f,13.591838f,-21.073362f,2.5877955f,-20.91351f),
            arrayOf(-12.41657f,0.9702136f,-16.622057f,9.386484f,-8.883725f,33.42575f,-1.191239f,-19.695387f,-5.0235033f,20.611315f),
            arrayOf(-22.539385f,8.391984f,-7.6859827f,-15.905986f,0.21990234f,18.601997f,-3.1677938f,11.272519f,23.7799f,-12.620032f),
            arrayOf(3.500333f,16.715744f,-10.053369f,-3.7577589f,15.344035f,-7.7219176f,8.326493f,-17.384546f,-6.936204f,2.5681005f),
            arrayOf(44.3191f,13.169592f,-9.176421f,-4.294819f,-4.0085244f,-10.655659f,20.376423f,-43.95753f,-6.0898576f,0.79027903f),
            arrayOf(-0.31953922f,0.5663047f,-6.8197327f,-0.49154052f,5.9277463f,-10.193342f,6.9950376f,3.4462943f,0.5911596f,0.91379553f),
            arrayOf(-27.587473f,-17.620731f,-24.862154f,-0.069514774f,52.387928f,-6.2613044f,-9.535869f,26.321228f,8.094414f,-0.23759374f),
            arrayOf(23.551098f,2.2310472f,14.172897f,3.8004076f,-23.01119f,-9.11606f,-3.607278f,2.0929816f,-7.4567633f,-2.1139364f),
            arrayOf(-22.236814f,3.0116785f,1.2733096f,2.889211f,-25.20383f,18.6995f,23.111809f,-8.736698f,-0.5953214f,8.141141f),
            arrayOf(-20.325918f,-13.726747f,-9.333914f,1.807587f,17.888416f,11.058888f,18.356342f,-21.769115f,8.820314f,7.685048f),
            arrayOf(-5.8426647f,-23.207632f,1.4261566f,-0.76760614f,13.007307f,2.969682f,2.9243288f,10.726689f,-0.002107197f,-0.8355663f),
            arrayOf(8.339395f,-18.193815f,4.508161f,-16.213743f,12.156332f,-18.822018f,-24.6831f,55.869102f,-3.0674372f,0.569467f),
            arrayOf(17.916647f,40.076412f,15.951899f,0.980472f,-16.736584f,-12.730901f,-15.816354f,-3.37768f,-9.004544f,-16.72071f)
        )
    }
}