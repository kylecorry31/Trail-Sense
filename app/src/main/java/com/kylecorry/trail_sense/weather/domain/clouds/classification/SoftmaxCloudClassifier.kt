package com.kylecorry.trail_sense.weather.domain.clouds.classification

import android.graphics.Bitmap
import android.graphics.Rect
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.glcm
import com.kylecorry.andromeda.core.bitmap.ColorChannel
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.classifiers.LogisticRegressionClassifier
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.math.statistics.Texture
import com.kylecorry.sol.math.statistics.TextureFeatures
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.shared.ClassificationResult
import com.kylecorry.trail_sense.shared.colors.ColorUtils
import kotlin.math.sqrt

class SoftmaxCloudClassifier(
    private val onFeaturesCalculated: (List<Float>) -> Unit = {}
) : ICloudClassifier {

    override suspend fun classify(bitmap: Bitmap): List<ClassificationResult<CloudGenus?>> {
        var averageNRBR = 0.0

        for (w in 0 until bitmap.width) {
            for (h in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(w, h)
                averageNRBR += ColorUtils.nrbr(pixel)
            }
        }

        averageNRBR /= bitmap.width * bitmap.height

        val regions = mutableListOf<Rect>()
        for (x in 0 until bitmap.width step GLCM_WINDOW_SIZE) {
            for (y in 0 until bitmap.height step GLCM_WINDOW_SIZE) {
                regions.add(Rect(x, y, x + GLCM_WINDOW_SIZE, y + GLCM_WINDOW_SIZE))
            }
        }

        val textures = regions.map {
            val glcm = bitmap.glcm(
                listOf(
                    0 to GLCM_STEP_SIZE,
                    GLCM_STEP_SIZE to GLCM_STEP_SIZE,
                    GLCM_STEP_SIZE to 0,
                    GLCM_STEP_SIZE to -GLCM_STEP_SIZE
                ),
                ColorChannel.Red,
                normed = true,
                symmetric = true,
                levels = GLCM_LEVELS,
                region = it
            )
            Texture.features(glcm)
        }

        val texture = TextureFeatures(
            textures.map { it.energy }.avg(),
            textures.map { it.entropy }.avg(),
            textures.map { it.contrast }.avg(),
            textures.map { it.homogeneity }.avg(),
            textures.map { it.dissimilarity }.avg(),
            textures.map { it.angularSecondMoment }.avg(),
            textures.map { it.horizontalMean }.avg(),
            textures.map { it.verticalMean }.avg(),
            textures.map { it.horizontalVariance }.avg(),
            textures.map { it.verticalVariance }.avg(),
            textures.map { it.correlation }.avg(),
            textures.map { it.max }.avg(),
        )

        val features = listOf(
            // Color
            SolMath.norm(averageNRBR.toFloat(), -1f, 1f) * 2,
            // Texture
            texture.energy,
            texture.contrast,
            SolMath.norm(texture.verticalMean, 0f, GLCM_LEVELS.toFloat()),
            SolMath.norm(sqrt(texture.verticalVariance), 0f, 3f),
            // Bias
            1f
        )

        onFeaturesCalculated(features)

        val isClear = features[0] < 0.55 && features[4] < 0.15f

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
                confidence * (if (isClear) 0.5f else 1f)
            )
        } + listOf(
            ClassificationResult<CloudGenus?>(
                null,
                if (isClear) 0.5f else 0f
            )
        )


        return result.sortedByDescending { it.confidence }
    }

    private fun List<Float>.avg(): Float {
        return Statistics.mean(this)
    }

    companion object {
        const val IMAGE_SIZE = 400
        private const val GLCM_LEVELS = 16
        private const val GLCM_WINDOW_SIZE = IMAGE_SIZE / 4
        private const val GLCM_STEP_SIZE = 1
        private val weights = arrayOf(
            arrayOf(
                -30.499298f,
                -32.44622f,
                8.59708f,
                24.778687f,
                -16.14332f,
                26.324936f,
                16.153542f,
                -4.4983864f,
                16.956928f,
                -8.88946f
            ),
            arrayOf(
                -18.911097f,
                -30.558966f,
                21.984447f,
                -5.2983747f,
                -19.40249f,
                21.391472f,
                6.4511223f,
                9.318568f,
                -0.7885379f,
                16.439323f
            ),
            arrayOf(
                2.7423918f,
                7.5182056f,
                -0.55555236f,
                -19.869183f,
                7.2663717f,
                8.8256645f,
                -2.1093395f,
                -1.5292113f,
                -0.4180131f,
                -1.3353842f
            ),
            arrayOf(
                -0.07265506f,
                8.69243f,
                -15.892311f,
                -17.148743f,
                8.161453f,
                2.6490138f,
                0.9795754f,
                -12.541805f,
                8.29234f,
                17.448767f
            ),
            arrayOf(
                -12.927965f,
                -14.036783f,
                7.5850844f,
                -6.566447f,
                -5.2669687f,
                -6.070137f,
                14.993839f,
                17.069252f,
                -12.29734f,
                17.991028f
            ),
            arrayOf(
                42.331375f,
                44.738117f,
                -14.025002f,
                -6.911371f,
                23.916212f,
                -36.44089f,
                -20.645504f,
                -0.49328813f,
                -15.2304f,
                -16.924234f
            )
        )
    }
}