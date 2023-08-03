package com.kylecorry.trail_sense.weather.domain.clouds.classification

import android.graphics.Bitmap
import androidx.core.graphics.blue
import com.kylecorry.sol.math.classifiers.LogisticRegressionClassifier
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.shared.ClassificationResult

class LBPCloudClassifier(
    private val onFeaturesCalculated: (List<Float>) -> Unit = {}
) : ICloudClassifier {

    override suspend fun classify(bitmap: Bitmap): List<ClassificationResult<CloudGenus?>> {
        val lbp = getLbpHistogram(bitmap)

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

    private fun getLbpHistogram(bitmap: Bitmap): FloatArray {
        val width = bitmap.width
        val height = bitmap.height
        val histogram = FloatArray(10)
        var total = 0f

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
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
        const val IMAGE_SIZE = 100
        private val weights = arrayOf(
            arrayOf(
                -4.10906f,
                7.3541436f,
                -6.271827f,
                -2.14748f,
                8.982099f,
                -7.162201f,
                2.879358f,
                3.4205294f,
                -2.1039586f,
                -0.21977752f
            ),
            arrayOf(
                1.3694375f,
                7.5818477f,
                -7.6387086f,
                -3.4121773f,
                10.471735f,
                -12.139254f,
                3.613723f,
                4.3380785f,
                -3.371358f,
                -0.33248425f
            ),
            arrayOf(
                -6.4845824f,
                -2.863373f,
                -8.208708f,
                -2.941507f,
                16.153568f,
                -9.436749f,
                6.229327f,
                10.605197f,
                -1.6151258f,
                -1.062369f
            ),
            arrayOf(
                -11.196043f,
                -8.09754f,
                -10.697432f,
                -2.5413766f,
                23.006367f,
                -6.9467034f,
                6.30805f,
                15.4449005f,
                -1.498438f,
                -3.2007532f
            ),
            arrayOf(
                -0.17485349f,
                -4.919042f,
                3.3450282f,
                0.26755652f,
                -0.9955937f,
                -5.035445f,
                8.925254f,
                9.019802f,
                -3.3444333f,
                -6.7549076f
            ),
            arrayOf(
                -2.2085962f,
                -6.233697f,
                8.238033f,
                2.4505768f,
                -12.853155f,
                12.543199f,
                8.2696705f,
                -6.4263916f,
                -0.1349742f,
                -3.3654168f
            ),
            arrayOf(
                -1.12682f,
                -3.1049178f,
                3.0033596f,
                1.8207102f,
                -3.977123f,
                10.368069f,
                3.787785f,
                -12.503427f,
                3.2211044f,
                -1.0239335f
            ),
            arrayOf(
                4.9189553f,
                -1.1998445f,
                5.994802f,
                0.82775027f,
                -9.4168f,
                12.142638f,
                -11.005945f,
                -6.7099657f,
                3.839352f,
                1.0188689f
            ),
            arrayOf(
                -3.1157148f,
                -14.570764f,
                2.518044f,
                1.3974302f,
                -20.13756f,
                17.396652f,
                -13.059538f,
                21.4053f,
                3.2298145f,
                5.4613433f
            ),
            arrayOf(
                14.747043f,
                19.850399f,
                -2.2614298f,
                -9.129237f,
                13.861215f,
                -19.766956f,
                -4.7340417f,
                -5.755801f,
                -4.6618457f,
                -1.5865831f
            )
        )
    }
}