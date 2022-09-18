package com.kylecorry.trail_sense.weather.domain.clouds.classification

import android.graphics.Bitmap
import android.graphics.Color
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
        var darkness = 0.0

        for (w in 0 until bitmap.width) {
            for (h in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(w, h)
                averageNRBR += ColorUtils.nrbr(pixel)
                darkness += Color.blue(pixel)
            }
        }

        averageNRBR /= bitmap.width * bitmap.height
        darkness /= bitmap.width * bitmap.height

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
            SolMath.norm(darkness.toFloat(), 0f, 255f),
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
                -23.590385283860275f,
                -12.878197687650868f,
                -1.8089660768115572f,
                5.3213117044138345f,
                3.256097558630317f,
                24.563627849459586f,
                25.525458092793492f,
                -27.06583429448029f,
                8.899954410331198f,
                -1.9559503360905768f
            ),
            arrayOf(
                10.229628331651195f,
                2.5557881303681302f,
                -0.25430563130985673f,
                -5.777905059519628f,
                10.644368531063675f,
                -0.8800658476446017f,
                -7.473367732073277f,
                -8.01881885410084f,
                -1.7463650301877915f,
                0.1986570023700655f
            ),
            arrayOf(
                -2.3211721455208703f,
                -20.86676344583415f,
                9.8062435706812f,
                3.9069813746717754f,
                -11.827842541584502f,
                8.220238353420413f,
                -2.567231953415041f,
                11.88267993474244f,
                3.070358153245062f,
                0.6139097652330388f
            ),
            arrayOf(
                -4.3363284311063195f,
                25.350006795505607f,
                -6.192472883258766f,
                -2.752686754831529f,
                23.77236233908768f,
                -10.891061568251493f,
                -19.06005920120243f,
                -7.6830610105824295f,
                -2.036828587612685f,
                3.8924355668106676f
            ),
            arrayOf(
                -4.5057660802776205f,
                -0.12604633252403427f,
                -3.271321394410598f,
                -6.440769368615688f,
                -3.9538930591943444f,
                3.5573986342942088f,
                9.334350058685803f,
                5.123483591798314f,
                -1.7640415722416827f,
                2.0872571537926103f
            ),
            arrayOf(
                1.8287548988021192f,
                -17.45794341569634f,
                -1.7056952472502602f,
                -3.3539920178470757f,
                -1.9692954293617997f,
                -12.290552247075114f,
                19.31661602173667f,
                25.977081293334162f,
                -7.876209793096056f,
                -2.52669940860955f
            ),
            arrayOf(
                8.074944225595898f,
                19.649911811845246f,
                -2.198774965487402f,
                1.7906831186990473f,
                1.7914603713919812f,
                -14.288849876051462f,
                -12.156980071672495f,
                1.0595155664169953f,
                -2.5263022076371833f,
                -1.595863227943878f
            )
        )
    }
}