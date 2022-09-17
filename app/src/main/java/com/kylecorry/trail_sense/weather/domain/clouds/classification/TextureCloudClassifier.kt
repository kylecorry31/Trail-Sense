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
                -15.690427447074558f,
                -10.487093238528942f,
                -0.11964586197917493f,
                4.652132235366456f,
                -4.8467130847957005f,
                6.895142972120733f,
                24.349389862364948f,
                -6.295196113202429f,
                2.2836030032349655f,
                -0.8727120626003579f
            ),
            arrayOf(
                -4.4419030955898755f,
                -13.255877789779412f,
                4.38479450259242f,
                1.7770788503646826f,
                -6.389408652334074f,
                8.83754520963699f,
                -8.767924108439454f,
                11.184245662861487f,
                9.111122029387655f,
                -2.6532938624107594f
            ),
            arrayOf(
                -0.863618390241976f,
                17.666336568386306f,
                -4.73423979646147f,
                -2.498103375222573f,
                9.524611608011973f,
                -6.497898353603554f,
                -7.96911462620511f,
                -1.3943771458421081f,
                -2.133464726153575f,
                -1.1874068084881444f
            ),
            arrayOf(
                -9.81008524265961f,
                -6.659081506784471f,
                -5.443785936519121f,
                5.426577899327816f,
                -4.784584059278766f,
                12.29288636747075f,
                6.505624754213405f,
                -7.8462247902636895f,
                9.248401523566795f,
                0.9666767037589515f
            ),
            arrayOf(
                -3.225075416486053f,
                -9.384435587979688f,
                -2.7456155066379675f,
                -6.277611371114981f,
                5.278839292025862f,
                -8.439305055266944f,
                10.63271721556228f,
                23.6834107472337f,
                -11.349694708485353f,
                1.904108840504215f
            ),
            arrayOf(
                15.555615579927215f,
                16.77530534015229f,
                1.4726429986923952f,
                -4.6263520829222315f,
                6.954878498667065f,
                -12.973676017607826f,
                -8.727248579514452f,
                -5.134409935452758f,
                -9.996844935492998f,
                0.6388300388157928f
            )
        )
    }
}