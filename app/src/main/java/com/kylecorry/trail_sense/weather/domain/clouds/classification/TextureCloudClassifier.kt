package com.kylecorry.trail_sense.weather.domain.clouds.classification

import android.graphics.Bitmap
import android.graphics.Rect
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.glcm
import com.kylecorry.andromeda.core.bitmap.ColorChannel
import com.kylecorry.sol.math.classifiers.LogisticRegressionClassifier
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.math.statistics.Texture
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.shared.ClassificationResult
import com.kylecorry.trail_sense.weather.domain.clouds.mask.ICloudPixelClassifier
import com.kylecorry.trail_sense.weather.domain.clouds.mask.SkyPixelClassification
import kotlin.math.sqrt

/**
 * A cloud classifier using the method outlined in: doi:10.5194/amt-3-557-2010
 */
class TextureCloudClassifier(
    private val pixelClassifier: ICloudPixelClassifier,
    private val onFeaturesCalculated: (List<Float>) -> Unit = {}
) : ICloudClassifier {

    override suspend fun classify(bitmap: Bitmap): List<ClassificationResult<CloudGenus>> {
        var skyPixels = 0
        var cloudPixels = 0

        for (w in 0 until bitmap.width) {
            for (h in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(w, h)
                when (pixelClassifier.classify(pixel)) {
                    SkyPixelClassification.Sky -> {
                        skyPixels++
                    }
                    SkyPixelClassification.Obstacle -> {
                        // Do nothing
                    }
                    else -> {
                        cloudPixels++
                    }
                }
            }
        }

        val levels = 16
        val step = 1
        val windowSize = 50
        val regions = mutableListOf<Rect>()
        for (x in 0 until bitmap.width step windowSize) {
            for (y in 0 until bitmap.height step windowSize) {
                regions.add(Rect(x, y, x + windowSize, y + windowSize))
            }
        }

        val textures = regions.map {
            val glcm = bitmap.glcm(
                listOf(
                    0 to step,
                    step to step,
                    step to 0,
                    step to -step
                ),
                ColorChannel.Red,
                excludeTransparent = true,
                normed = true,
                symmetric = true,
                levels = levels,
                region = it
            )
            Texture.features(glcm)
        }

        val cover = if (skyPixels + cloudPixels != 0) {
            cloudPixels / (skyPixels + cloudPixels).toFloat()
        } else {
            0f
        }

        if (cover < 0.05) {
            return emptyList()
        }

        val features = listOf(
            cover,
            // Color
            // Texture
            Statistics.median(textures.map { it.energy }),
            Statistics.median(textures.map { it.contrast }),
            Statistics.median(textures.map { it.verticalMean / levels.toFloat() }),
            sqrt(Statistics.median(textures.map { it.verticalVariance })),
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
        private val weights = arrayOf(
            arrayOf(
                -7.250256463037756f,
                -4.147736274525556f,
                0.3755199753075953f,
                4.589243099539504f,
                -1.845928008834235f,
                4.908645138471656f,
                11.219120709404606f,
                -6.790296822305672f,
                4.271255236243025f,
                -5.078931339468556f
            ),
            arrayOf(
                -1.5443827498507463f,
                -9.414194411603633f,
                1.412629830665699f,
                1.1455987001818895f,
                -1.7250537633351157f,
                3.4211658160901997f,
                -6.704610844974213f,
                10.842933858607733f,
                3.300899290292573f,
                -0.42218175734462415f
            ),
            arrayOf(
                -1.7397053593412013f,
                11.005394534868476f,
                -1.3856443667900888f,
                -0.7244740607361306f,
                6.148813870397207f,
                -3.830943978456825f,
                -6.174615895990192f,
                -1.9859222652914816f,
                -0.23274256964738435f,
                -1.072136722553462f
            ),
            arrayOf(
                -3.047097033236946f,
                -2.526716507126389f,
                -5.02673259321876f,
                2.046949479515713f,
                -1.9097106427044819f,
                4.83109845838557f,
                0.8981684319382629f,
                0.3592890429469326f,
                1.2063343359270386f,
                2.678338055853715f
            ),
            arrayOf(
                0.7561704668286304f,
                -1.0461089999242144f,
                0.7109376794759613f,
                -6.892539149836536f,
                4.267063946413256f,
                -7.278889684846466f,
                6.697499363791351f,
                9.033987638940351f,
                -9.335215796146226f,
                3.1192129065102945f
            ),
            arrayOf(
                9.762526131231256f,
                10.592115720239878f,
                1.5126671331019226f,
                -1.9905054771733097f,
                1.0402367572484648f,
                -4.535581536212689f,
                -8.537721793456281f,
                -6.396546373637519f,
                -2.2502285030993576f,
                0.8653463458293095f
            )
        )
    }
}