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
                -15.481049012799835f,
                -14.669745936035273f,
                5.167834184242672f,
                6.124313746651251f,
                -2.0996118027085053f,
                4.123517942193369f,
                14.339881854652539f,
                -2.1752341954534367f,
                7.423311769902283f,
                -3.0822089764651888f
            ),
            arrayOf(
                -6.733249278787395f,
                -18.750838934705307f,
                8.921196989044505f,
                -1.92929056380659f,
                -12.34267560291782f,
                13.169259423602357f,
                2.9017383872669584f,
                11.893646504347107f,
                -0.16708517998598962f,
                3.187438384108881f
            ),
            arrayOf(
                -0.2030938111908104f,
                5.433928690181039f,
                -0.7607091434869054f,
                -5.735116026418656f,
                5.394497536765065f,
                6.340629136255869f,
                -2.101392763873266f,
                -1.3554348947222206f,
                -3.337825715070501f,
                -3.832225375994764f
            ),
            arrayOf(
                -7.547200650243388f,
                0.4605113961886332f,
                -10.261284152757254f,
                -2.297022218448516f,
                3.713900030461383f,
                16.25184141683353f,
                2.3510280849152765f,
                -5.60639238989148f,
                3.5611791713387757f,
                -0.21362020397770792f
            ),
            arrayOf(
                -6.481677042776856f,
                -9.783923993386848f,
                -1.354494519714624f,
                -2.8866802802468743f,
                -2.4118658270121496f,
                -2.711638825842957f,
                10.848799937490808f,
                16.272143963020035f,
                -8.946404311855733f,
                7.833183029916746f
            ),
            arrayOf(
                23.765157976985336f,
                25.71895402539638f,
                -3.475784549742715f,
                -2.5465846476016654f,
                8.41930868725635f,
                -19.213598994971722f,
                -17.0585150915302f,
                -7.8197989837072726f,
                -5.479950277468049f,
                -2.599483278398578f
            )
        )
    }
}