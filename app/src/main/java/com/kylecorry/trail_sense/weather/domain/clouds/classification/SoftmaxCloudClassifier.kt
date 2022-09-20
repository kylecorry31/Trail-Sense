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
                -16.060884f,
                -15.959107f,
                2.1524954f,
                6.482405f,
                -4.2587667f,
                11.4224205f,
                14.302987f,
                0.86549675f,
                5.648574f,
                -4.0885253f
            ),
            arrayOf(
                -7.9350467f,
                -18.648846f,
                12.850908f,
                -2.0087419f,
                -11.82254f,
                10.452887f,
                2.1654541f,
                10.751538f,
                -0.2743768f,
                5.006465f
            ),
            arrayOf(
                0.17029418f,
                4.853572f,
                -0.24241693f,
                -5.704879f,
                4.528988f,
                7.5379386f,
                -2.9304395f,
                -3.0987773f,
                -3.2110298f,
                -1.2607244f
            ),
            arrayOf(
                -6.323694f,
                1.1692586f,
                -7.7031794f,
                -1.9249102f,
                3.2451253f,
                8.791285f,
                3.4040418f,
                -11.031382f,
                5.846891f,
                5.2376313f
            ),
            arrayOf(
                -7.949422f,
                -8.314046f,
                1.8227433f,
                -2.751219f,
                -1.9908776f,
                -11.103702f,
                11.10057f,
                16.3878f,
                -6.28156f,
                9.693888f
            ),
            arrayOf(
                25.069473f,
                25.931606f,
                -5.4964123f,
                -2.6729715f,
                10.620847f,
                -17.921286f,
                -17.087984f,
                -6.6766043f,
                -5.6899695f,
                -5.742053f
            )
        )
    }
}