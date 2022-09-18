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
        const val IMAGE_SIZE = 500
        private const val GLCM_LEVELS = 16
        private const val GLCM_WINDOW_SIZE = 100
        private const val GLCM_STEP_SIZE = 1
        private val weights = arrayOf(
            arrayOf(
                -25.89520342466119f,
                -12.84558073826224f,
                -1.7632580904171875f,
                5.302998123258587f,
                2.8668030025643727f,
                25.16637039134395f,
                26.67612130150181f,
                -26.330203464013803f,
                9.250770414599007f,
                -1.8594528514767052f
            ),
            arrayOf(
                -2.4492072638174336f,
                -20.822154606404393f,
                9.947666992696957f,
                2.79032544438528f,
                -11.314630939867774f,
                9.061995570243443f,
                -2.6291826077726737f,
                11.358351884525693f,
                2.5359978513407464f,
                0.5269058270129142f
            ),
            arrayOf(
                -4.419044126871426f,
                26.193534877817296f,
                -6.180677746580912f,
                -2.7544090786326003f,
                24.89075403228848f,
                -10.926238451252965f,
                -19.67794787047388f,
                -8.741820773847182f,
                -1.8910520119231915f,
                3.8317328219111637f
            ),
            arrayOf(
                4.671596508049094f,
                1.5454270638241296f,
                -3.9326603497657335f,
                -8.921295520112754f,
                3.9332634015965855f,
                2.104551200721596f,
                3.366701257796253f,
                -1.641588686399821f,
                -3.124069643651549f,
                2.0182062480791516f
            ),
            arrayOf(
                0.8657360899253382f,
                -17.44214186128198f,
                -1.611281008262448f,
                -3.402788270091211f,
                -2.817534978120088f,
                -12.343297609028033f,
                20.18891706998467f,
                26.791268071832793f,
                -7.771357739316704f,
                -2.4847461530341746f
            ),
            arrayOf(
                11.060379736852422f,
                20.03840272584401f,
                -2.466970444280833f,
                0.5591638477058274f,
                4.14190287850429f,
                -14.792628512007953f,
                -14.20762497397164f,
                -0.11941655981688329f,
                -2.7727190769616232f,
                -1.4860383610518277f
            )
        )
    }
}