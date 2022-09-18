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
        const val IMAGE_SIZE = 500
        private const val GLCM_LEVELS = 16
        private const val GLCM_WINDOW_SIZE = 100
        private const val GLCM_STEP_SIZE = 1
        private val weights = arrayOf(
            arrayOf(
                -15.326726651480232f,
                -8.433998127591156f,
                -2.3192139197485977f,
                6.323527068303827f,
                0.4642374289465873f,
                16.08378124535421f,
                14.483817720604844f,
                -16.295838258396994f,
                8.116210630685988f,
                -2.6733617115291817f
            ),
            arrayOf(
                -1.6255601813818528f,
                -19.058845879630287f,
                10.360483777547705f,
                1.76233369029118f,
                -10.455771553444697f,
                7.177126405323579f,
                -3.092183498114298f,
                12.265182222315566f,
                1.7209098812005081f,
                1.2802747059555777f
            ),
            arrayOf(
                -2.5815078058346184f,
                22.8321737569322f,
                -6.840490408939233f,
                -4.032531723597181f,
                21.709560897158564f,
                -14.517484587362082f,
                -15.45695404604441f,
                -4.381019900806199f,
                -2.3438156573242366f,
                5.720530868689756f
            ),
            arrayOf(
                3.5774640759763763f,
                0.8547847304306743f,
                -4.614705693794183f,
                -9.138071003692222f,
                3.522081932272075f,
                4.068668908946679f,
                4.506009513976177f,
                -2.540978676911561f,
                -2.402175532172203f,
                2.089417140710355f
            ),
            arrayOf(
                1.8068973358086673f,
                -18.254462396466582f,
                -0.6466148315888837f,
                -3.9836138714576523f,
                -4.555304015359183f,
                -13.244013503570946f,
                21.896395292562612f,
                27.56782004262448f,
                -8.450818569537757f,
                -2.124757517607184f
            ),
            arrayOf(
                12.770553963123204f,
                21.00278063249535f,
                -0.17570696288951232f,
                -1.424496561338219f,
                5.204872030863934f,
                -17.706225004990245f,
                -15.985819822771278f,
                2.296576138532642f,
                -5.027754783664188f,
                -0.6565178209404253f
            )
        )
    }
}