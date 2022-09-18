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
                ColorChannel.Blue,
                normed = true,
                symmetric = true,
                levels = GLCM_LEVELS,
                region = it
            )
            Texture.features(glcm)
        }

        val features = listOf(
            // Color
            SolMath.norm(averageNRBR.toFloat(), -1f, 1f) * 2,
            // Texture
            Statistics.median(textures.map { it.energy }),
            Statistics.median(textures.map { it.contrast }),
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

        val isClear = features[4] < 0.15f

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

    companion object {
        const val IMAGE_SIZE = 500
        private const val GLCM_LEVELS = 16
        private const val GLCM_WINDOW_SIZE = 100
        private const val GLCM_STEP_SIZE = 1
        private val weights = arrayOf(
            arrayOf(
                -11.547071451503827f,
                -5.115500780328104f,
                0.2910332249450597f,
                1.5564150086077198f,
                3.398955758589519f,
                7.6248515193988f,
                10.816267339969862f,
                -9.213037081918916f,
                3.1280520755999612f,
                -0.7523284601086945f
            ),
            arrayOf(
                0.24338589066452004f,
                -6.671817905040107f,
                7.286939915228218f,
                0.5209459615138191f,
                -7.106403528607966f,
                5.349015156309769f,
                -6.145826168722913f,
                4.617403631335282f,
                1.5130817654547768f,
                0.6893930332403198f
            ),
            arrayOf(
                -1.2070819945607896f,
                11.895088640242566f,
                -3.197145539877684f,
                -1.3602310846585581f,
                9.740528367208151f,
                -5.5245689733975265f,
                -7.200946982029556f,
                -1.6640359696405833f,
                -1.7438036581719285f,
                -0.3480005911674311f
            ),
            arrayOf(
                4.316402459100921f,
                3.544179091004945f,
                -4.034130363501316f,
                -2.8702676013149966f,
                1.769685384728576f,
                1.1380830555862878f,
                -1.6315452268659139f,
                -0.5091664821803447f,
                -1.8423320498298612f,
                -0.3760520725601046f
            ),
            arrayOf(
                0.31370864015881134f,
                -4.966264259326547f,
                -4.053860755542305f,
                -1.5485712384570751f,
                0.5458379709702794f,
                -7.834422348097236f,
                7.798293986119925f,
                14.180283097350312f,
                -3.5929516545524476f,
                -1.4553509849377053f
            ),
            arrayOf(
                7.701931353996819f,
                6.642761757196731f,
                -0.9129722735345788f,
                -1.05700965800932f,
                0.49968105607821445f,
                -8.33166129893201f,
                -4.417204946051044f,
                3.129815856173385f,
                -2.145288495218916f,
                -0.8955201707852971f
            )
        )
    }
}