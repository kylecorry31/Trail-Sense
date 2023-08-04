package com.kylecorry.trail_sense.weather.domain.clouds.classification

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.core.graphics.red
import com.kylecorry.sol.math.classifiers.LogisticRegressionClassifier
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.shared.ClassificationResult
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class MultiscaleLBPCloudClassifier(
    private val onFeaturesCalculated: (List<Float>) -> Unit = {}
) : ICloudClassifier {

    override suspend fun classify(bitmap: Bitmap): List<ClassificationResult<CloudGenus?>> {

        val lbpSmallScale = lbp(bitmap, 1.0)
        val lbpLargeScale = lbp(bitmap, 5.0)

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
    }

    private fun lbp(bitmap: Bitmap, radius: Double = 1.0, windowSize: Int? = null): FloatArray {
        if (windowSize == null) {
            return getLbpHistogram(bitmap, null, getNeighborOffsets(radius))
        }

        val regions = mutableListOf<Rect>()
        for (x in 0 until bitmap.width step windowSize) {
            for (y in 0 until bitmap.height step windowSize) {
                regions.add(Rect(x, y, x + windowSize, y + windowSize))
            }
        }

        val offsets = getNeighborOffsets(radius)
        val histograms = regions.map {
            getLbpHistogram(bitmap, it, offsets)
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

    private fun getLbpHistogram(
        bitmap: Bitmap,
        region: Rect? = null,
        offsets: List<Pair<Int, Int>> = getNeighborOffsets(1.0)
    ): FloatArray {
        val width = bitmap.width
        val height = bitmap.height
        val histogram = FloatArray(10)
        var total = 0f

        val maxOffset = offsets.maxOf { maxOf(it.first, it.second) }

        val startX = region?.left?.coerceAtLeast(maxOffset) ?: maxOffset
        val startY = region?.top?.coerceAtLeast(maxOffset) ?: maxOffset
        val endX = region?.right?.coerceAtMost(width - maxOffset) ?: (width - maxOffset)
        val endY = region?.bottom?.coerceAtMost(height - maxOffset) ?: (height - maxOffset)


        for (y in startY until endY) {
            for (x in startX until endX) {
                val lbpValue = getInvariantLbpValue(bitmap, x, y, offsets)
                histogram[lbpValue]++
                total++
            }
        }

        for (i in histogram.indices) {
            histogram[i] /= total
        }

        return histogram
    }


    private fun getInvariantLbpValue(
        bitmap: Bitmap,
        x: Int,
        y: Int,
        offsets: List<Pair<Int, Int>>
    ): Int {
        val centerPixel = bitmap.getPixel(x, y)
        var ones = 0
        var lastBit = false
        var firstBit = false
        var transitions = 0

        val center = centerPixel.red

        for (i in offsets.indices) {
            val neighbor = bitmap.getPixel(x + offsets[i].first, y + offsets[i].second)
            val greater = neighbor.red >= center
            if (greater) {
                ones++
            }
            if (i == 0) {
                firstBit = greater
            } else if (i == offsets.lastIndex) {
                if (greater != lastBit) transitions++
                if (greater != firstBit) transitions++
            } else if (greater != lastBit) {
                transitions++
            }
            lastBit = greater
        }

        return if (transitions <= 2) ones else 9
    }

    private fun getNeighborOffsets(radius: Double): List<Pair<Int, Int>> {
        val numPoints = 8
        val offsets = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until numPoints) {
            val angle = 2 * PI * i / numPoints
            val x = radius * cos(angle)
            val y = radius * sin(angle)
            offsets.add(Pair(x.roundToInt(), y.roundToInt()))
        }
        return offsets
    }


    companion object {
        const val IMAGE_SIZE = 400
        const val IMAGE_SIZE_LARGE_SCALE = 100
        private const val WINDOW_SIZE_SMALL_SCALE = IMAGE_SIZE / 4
        private const val WINDOW_SIZE_LARGE_SCALE = IMAGE_SIZE_LARGE_SCALE / 4
        private val weights = arrayOf(
            arrayOf(-4.545511f,13.800323f,-14.081253f,-3.6412048f,6.4762626f,-2.5266614f,9.610536f,-3.1358275f,-2.9644222f,1.5508752f),
            arrayOf(10.438164f,20.882975f,-10.470973f,0.58085954f,-11.467854f,4.961277f,-5.181354f,-10.807014f,-3.4993176f,5.0246763f),
            arrayOf(17.064995f,6.727053f,-9.271355f,-6.5672116f,4.431277f,-8.152893f,2.6728873f,-4.7798753f,-5.1762443f,3.5049965f),
            arrayOf(-20.45375f,-8.8936405f,-22.193487f,-8.6166935f,44.9864f,-27.046625f,16.87383f,26.234146f,-8.6076975f,8.332416f),
            arrayOf(17.606098f,-10.506829f,-27.653027f,-21.317152f,33.247883f,-54.660515f,50.49329f,14.042252f,-10.467446f,9.9184065f),
            arrayOf(-11.9675665f,12.196302f,3.0299077f,18.209713f,-5.217884f,-5.2951736f,-18.897963f,21.17157f,-8.408922f,-4.198887f),
            arrayOf(43.2703f,6.810288f,35.62771f,11.092865f,-11.395496f,-8.376616f,-39.83585f,-27.031885f,-4.6622014f,-4.9353447f),
            arrayOf(42.047634f,-38.524403f,52.01215f,-2.8399131f,-1.1209841f,-22.538103f,17.988314f,-23.537909f,3.410169f,-26.112373f),
            arrayOf(-22.473627f,-1.1667529f,-20.982935f,9.977379f,-7.357363f,42.803127f,-4.801257f,-14.957552f,-4.012658f,23.532925f),
            arrayOf(-25.246876f,3.32296f,-12.565598f,-17.356977f,-1.3445458f,24.393295f,-0.36021218f,13.579202f,27.942297f,-11.75107f),
            arrayOf(15.005948f,13.454452f,-15.629221f,-5.0159874f,25.46512f,-12.582835f,13.537132f,-30.603018f,-7.914577f,4.9494424f),
            arrayOf(59.72347f,19.926468f,-13.604916f,-4.4995165f,-11.752292f,-13.759323f,19.773596f,-50.134632f,-7.8357124f,2.6968143f),
            arrayOf(-13.708365f,-6.315825f,-4.914649f,-3.3488452f,18.600977f,-10.109349f,2.1394851f,8.032404f,8.31331f,1.9235543f),
            arrayOf(-39.28324f,-15.109934f,-22.097502f,1.127199f,53.47302f,2.1344748f,-19.903906f,33.96935f,9.8318615f,-3.4485106f),
            arrayOf(22.012997f,6.096421f,26.567585f,7.913859f,-21.721302f,-5.973033f,-12.158716f,-8.41044f,-10.246373f,-3.4232156f),
            arrayOf(-12.418239f,3.2717702f,-16.515377f,3.4335685f,-40.28525f,15.7003f,39.517708f,5.4480643f,-6.7512965f,9.04797f),
            arrayOf(-21.904476f,-6.179507f,-8.031224f,0.0559989f,18.188316f,7.57812f,24.743107f,-41.090736f,10.665343f,16.602499f),
            arrayOf(10.850959f,-49.26027f,6.244005f,-3.4461281f,8.01207f,-1.3282794f,9.58861f,17.245064f,3.2907178f,-0.6875341f),
            arrayOf(25.180182f,-7.352062f,2.327598f,-20.167097f,17.068754f,-31.66709f,-28.335323f,51.8646f,-5.451301f,-2.7521098f),
            arrayOf(0.3047355f,46.254894f,19.190851f,3.46935f,-15.673309f,-6.604365f,-20.277687f,4.607669f,-10.520321f,-20.005714f)
        )
    }
}