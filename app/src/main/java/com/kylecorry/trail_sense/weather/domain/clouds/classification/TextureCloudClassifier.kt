package com.kylecorry.trail_sense.weather.domain.clouds.classification

import android.annotation.SuppressLint
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

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    override suspend fun classify(bitmap: Bitmap): List<ClassificationResult<CloudGenus>> {
        var skyPixels = 0
        var cloudPixels = 0

        val cloudBitmap = bitmap.copy(bitmap.config, true)

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
            val glcm = cloudBitmap.glcm(
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

        cloudBitmap.recycle()

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
                -8.01027185348848f,
                -2.8462806476072253f,
                -0.5953116587301296f,
                4.278859589938971f,
                -3.2666049686222625f,
                4.508852426669927f,
                10.233039500202498f,
                -8.310144502299217f,
                5.066076436031103f,
                -1.528810236453286f
            ),
            arrayOf(
                4.3117449481429615f,
                -3.1680658496971814f,
                3.226356714422978f,
                -0.665117789764696f,
                -3.056779993684596f,
                1.4592987106679998f,
                -6.663355550244327f,
                3.9089888697764446f,
                2.5755924741169487f,
                -1.6382787435020225f
            ),
            arrayOf(
                0.5292947193643752f,
                9.530950751925797f,
                -0.7081803202215281f,
                -1.1608184870411422f,
                4.4554352219835325f,
                -1.418168987172771f,
                -5.265035302257626f,
                -5.682494205196052f,
                -1.011291263620131f,
                0.47550160064166697f
            ),
            arrayOf(
                -1.1213673629201377f,
                -4.2942714374614726f,
                -1.7588941158517446f,
                3.210467759816803f,
                -0.19080691594934712f,
                5.762750880284436f,
                0.44284452027197724f,
                -1.7387280255592419f,
                -0.4004396603725681f,
                0.33852747757160645f
            ),
            arrayOf(
                1.6481505646679648f,
                1.7804900314293692f,
                -4.556382575154839f,
                -6.557755575058822f,
                5.1647991473938815f,
                -9.191543878315157f,
                7.041122600914962f,
                10.017939702496472f,
                -7.92351168960803f,
                2.0559099722499714f
            ),
            arrayOf(
                4.33982012352236f,
                5.9717803419133f,
                -0.34732257213252765f,
                -2.4038308835269526f,
                4.041025687616834f,
                -4.8852495486964145f,
                -5.2738436191194715f,
                1.5516198661708456f,
                -3.579063191336928f,
                0.2798644093692847f
            )
        )
    }
}