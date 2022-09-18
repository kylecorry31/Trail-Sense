package com.kylecorry.trail_sense.weather.domain.clouds.classification

import android.graphics.Bitmap
import android.graphics.Rect
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.glcm
import com.kylecorry.andromeda.core.bitmap.ColorChannel
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.classifiers.LogisticRegressionClassifier
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.math.statistics.Texture
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.shared.ClassificationResult
import com.kylecorry.trail_sense.shared.colors.ColorUtils
import kotlin.math.sqrt

/**
 * A cloud classifier using the method outlined in: doi:10.5194/amt-3-557-2010
 */
class TextureCloudClassifier(
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
                excludeTransparent = true,
                normed = true,
                symmetric = true,
                levels = GLCM_LEVELS,
                region = it
            )
            Texture.features(glcm)
        }

        val features = listOf(
            // Color
            SolMath.norm(averageNRBR.toFloat(), -1f, 1f),
            // Texture
            Statistics.median(textures.map { it.energy }),
            SolMath.norm(Statistics.median(textures.map { it.contrast }), 0f, 1.5f),
            SolMath.norm(
                Statistics.median(textures.map { it.verticalMean }),
                0f,
                GLCM_LEVELS.toFloat()
            ),
            SolMath.norm(sqrt(Statistics.median(textures.map { it.verticalVariance })), 0f, 3f),
            // Bias
            1f
        )

        onFeaturesCalculated(features)

        if (features[4] < 0.1f) {
            return listOf(ClassificationResult<CloudGenus?>(null, 1f)) + CloudGenus.values().map {
                ClassificationResult(it, 0f)
            }
        }
        val classifier = LogisticRegressionClassifier(weights)

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
                confidence
            )
        }.sortedByDescending { it.confidence } + listOf(ClassificationResult<CloudGenus?>(null, 0f))


        return result
    }

    companion object {
        const val IMAGE_SIZE = 400
        private const val GLCM_LEVELS = 16
        private const val GLCM_WINDOW_SIZE = 50
        private const val GLCM_STEP_SIZE = 1
        private val weights = arrayOf(
            arrayOf(
                -25.301912227418875f,
                -18.643574572683992f,
                4.804891680686801f,
                4.5795778048436935f,
                7.711989624838524f,
                9.316704133312824f,
                22.820119361977873f,
                -8.15451286077698f,
                5.91780128142596f,
                -2.97786970691181f
            ),
            arrayOf(
                -8.986940539328149f,
                -18.26748287806681f,
                8.901277354304815f,
                1.2112026410455143f,
                -8.824866869998651f,
                11.663651818522796f,
                -3.2010224662144338f,
                14.469683531887052f,
                4.550277461234826f,
                -1.8118016190986985f
            ),
            arrayOf(
                -2.5011596240272373f,
                17.899407056061925f,
                -5.839273171968869f,
                -2.803960455290144f,
                13.588786568266078f,
                -7.379556587131587f,
                -8.322882353881813f,
                -3.6285862645257745f,
                0.07400172735873418f,
                -1.019609668610693f
            ),
            arrayOf(
                -5.920347899081299f,
                -2.7612789530180204f,
                -5.301072911828902f,
                -1.274701758271556f,
                -3.9075652904761857f,
                13.679517482092576f,
                3.494824513283382f,
                -5.300138990097928f,
                4.681902273623455f,
                2.5670201307271627f
            ),
            arrayOf(
                -8.664588324916473f,
                -16.942105611121814f,
                -1.8080384074853528f,
                -3.5468278861843467f,
                1.3294635468409732f,
                -9.641240530881067f,
                13.809345642474772f,
                27.274389222404157f,
                -6.7523327698025986f,
                4.8403904993439175f
            ),
            arrayOf(
                22.280227462670876f,
                23.61088749658286f,
                -4.136367509645848f,
                -2.7587434795479466f,
                4.442978959670547f,
                -17.297375386575137f,
                -10.809187929287008f,
                -8.027907670055182f,
                -7.163310986058574f,
                -0.4501808737510704f
            )
        )
    }
}