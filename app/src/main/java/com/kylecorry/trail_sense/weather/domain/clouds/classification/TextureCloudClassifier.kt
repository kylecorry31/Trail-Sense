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

    override suspend fun classify(bitmap: Bitmap): List<ClassificationResult<CloudGenus>> {
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
            ClassificationResult(
                genus,
                confidence
            )
        }.sortedByDescending { it.confidence }


        return result
    }

    companion object {
        const val IMAGE_SIZE = 400
        private const val GLCM_LEVELS = 16
        private const val GLCM_WINDOW_SIZE = 50
        private const val GLCM_STEP_SIZE = 1
        private val weights = arrayOf(
            arrayOf(
                -13.02802297485645f,
                -4.8928910026430374f,
                1.1873968557991594f,
                2.843122641068841f,
                -1.6610208802157038f,
                4.074353173237457f,
                16.83373066202042f,
                -5.886576478465538f,
                1.445592387846503f,
                -0.7271188659330753f
            ),
            arrayOf(
                -1.3481173252650023f,
                -9.695948697050072f,
                1.6601859143383941f,
                3.902077982817408f,
                -4.190505014836371f,
                6.251309785886855f,
                -8.883053725871655f,
                7.905690895636963f,
                6.55144074697563f,
                -1.9742005571871462f
            ),
            arrayOf(
                -1.642489948283554f,
                12.640609641784742f,
                -2.0725670062396584f,
                -2.511949182736662f,
                8.08965118808117f,
                -4.746995430867104f,
                -6.181738486028459f,
                -1.036624824021855f,
                -1.9115554217910815f,
                -0.5772751855090763f
            ),
            arrayOf(
                -7.835530379419983f,
                -5.132120476822591f,
                -3.490374938357374f,
                5.232834479017481f,
                -2.7697493867619434f,
                8.67341887138739f,
                5.717399674847507f,
                -6.1952489800622805f,
                4.329176810029505f,
                1.2194277282471122f
            ),
            arrayOf(
                -3.1378175901217857f,
                -5.401111536229517f,
                -1.5256289662826514f,
                -5.969624095420895f,
                5.7423424135815235f,
                -6.997464859618627f,
                5.972284092691329f,
                17.373706869882184f,
                -7.321325999079119f,
                1.5543523254913414f
            ),
            arrayOf(
                12.110068048189834f,
                10.776232838539801f,
                0.323110917152315f,
                -5.222049390718706f,
                2.9615518723215835f,
                -8.440511352370835f,
                -4.2683045066043315f,
                -2.62146016845962f,
                -6.126602180469509f,
                -0.09537140977377909f
            )
        )
    }
}