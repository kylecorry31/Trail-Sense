package com.kylecorry.trail_sense.weather.domain.clouds.classification

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.glcm
import com.kylecorry.andromeda.core.bitmap.ColorChannel
import com.kylecorry.sol.math.SolMath.map
import com.kylecorry.sol.math.classifiers.LogisticRegressionClassifier
import com.kylecorry.sol.math.statistics.Texture
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.shared.ClassificationResult
import com.kylecorry.trail_sense.weather.domain.clouds.mask.ICloudPixelClassifier
import com.kylecorry.trail_sense.weather.domain.clouds.mask.SkyPixelClassification

/**
 * A cloud classifier using the method outlined in: doi:10.5194/amt-3-557-2010
 */
class AMTCloudClassifier(
    private val pixelClassifier: ICloudPixelClassifier,
    private val onFeaturesCalculated: (List<Float>) -> Unit = {}
) : ICloudClassifier {

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    override suspend fun classify(bitmap: Bitmap): List<ClassificationResult<CloudGenus>> {
        var skyPixels = 0
        var cloudPixels = 0

        var redMean = 0.0
        var greenMean = 0.0
        var blueMean = 0.0

        val cloudBitmap = bitmap.copy(bitmap.config, true)

        for (w in 0 until bitmap.width) {
            for (h in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(w, h)

                when (pixelClassifier.classify(pixel)) {
                    SkyPixelClassification.Sky -> {
                        skyPixels++
                    }
                    SkyPixelClassification.Obstacle -> {
                        cloudBitmap.setPixel(w, h, Color.TRANSPARENT)
                    }
                    else -> {
                        cloudPixels++
                        redMean += Color.red(pixel)
                        blueMean += Color.blue(pixel)
                        greenMean += Color.green(pixel)
                    }
                }
            }
        }

        val levels = 32
        val step = 1

        // Allow setting window size
        val glcm = cloudBitmap.glcm(
            listOf(
                0 to step,
                step to step,
                step to 0,
                step to -step
            ),
            ColorChannel.Blue,
            excludeTransparent = true,
            normed = true,
            symmetric = true,
            levels = levels
        )
        cloudBitmap.recycle()
        val texture = Texture.features(glcm)

        val cover = if (skyPixels + cloudPixels != 0) {
            cloudPixels / (skyPixels + cloudPixels).toFloat()
        } else {
            0f
        }

        if (cloudPixels != 0) {
            redMean /= cloudPixels
            greenMean /= cloudPixels
            blueMean /= cloudPixels
        }

        if (cover < 0.05) {
            return emptyList()
        }

        val features = listOf(
            cover,
            // Color
            (redMean / 255).toFloat(),
            (blueMean / 255).toFloat(),
            percentDifference(redMean, greenMean),
            percentDifference(redMean, blueMean),
            percentDifference(greenMean, blueMean),
            // Texture
            texture.entropy / 16f,
            texture.contrast / levels.toFloat(),
            texture.verticalMean / levels.toFloat(),
            texture.correlation,
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

    private fun percentDifference(color1: Double, color2: Double): Float {
        return map((color1 - color2).toFloat(), -255f, 255f, 0f, 1f)
    }

    companion object {
        private val weights = arrayOf(
            arrayOf(
                -5.057464806367483f,
                -5.411295966176792f,
                0.012418699359434519f,
                6.2760515806004f,
                -4.563347079470772f,
                9.401760428627433f,
                5.04758221236154f,
                -11.021926380293946f,
                7.010062381570169f,
                -2.0843405366479435f
            ),
            arrayOf(
                -13.605115419734345f,
                -2.9329281613825087f,
                -3.5697586664865075f,
                2.7202379359822673f,
                1.0949077019368805f,
                2.8249923983101426f,
                1.3359528002998209f,
                11.060034334880264f,
                1.0859022919947017f,
                0.18163242148851796f
            ),
            arrayOf(
                8.24195817097587f,
                0.7049080904630168f,
                1.5673564419112964f,
                -0.5357945823603437f,
                0.6572625023132387f,
                -0.14077273911894023f,
                -7.275411170560537f,
                -3.120162728897589f,
                0.38626465440839636f,
                -0.1595172630261247f
            ),
            arrayOf(
                -0.08685564918030228f,
                0.9535152241495743f,
                -0.85889096002469f,
                -1.4756066638477494f,
                0.013831225032203607f,
                -0.9006026950462528f,
                0.5902380456986058f,
                2.84784634295513f,
                -1.114374002515207f,
                -0.029483739360400333f
            ),
            arrayOf(
                -8.509503883575816f,
                -0.09266411306728893f,
                -2.229737272730536f,
                0.010466345987272896f,
                1.1497575908522997f,
                -0.6033912187673565f,
                4.48340972543388f,
                6.853289803655368f,
                -0.9427456471967093f,
                0.02725488236764529f
            ),
            arrayOf(
                -6.108864200970441f,
                0.8843235615993846f,
                -1.3609688754335052f,
                -0.46228896362293115f,
                2.202878520234456f,
                -1.8845597164785264f,
                3.949887429641883f,
                4.145435051564102f,
                -1.5636500074946464f,
                0.11689522687672274f
            ),
            arrayOf(
                -1.0950494125446688f,
                7.887321850169209f,
                -1.0054240434318946f,
                -3.899144652588313f,
                3.583516682668419f,
                -9.832638017104165f,
                6.557086008232251f,
                1.8978140488482147f,
                -4.85979022169562f,
                0.7219099893167191f
            ),
            arrayOf(
                -1.9569238841206966f,
                1.4932626828432405f,
                0.2810472318613391f,
                -1.3195068483366637f,
                3.8139512457555833f,
                -3.0382751420574556f,
                2.38163776494011f,
                -0.468226705059459f,
                -1.2456109616488231f,
                -0.2502449871160575f
            ),
            arrayOf(
                3.5738036793824186f,
                -5.89751061005609f,
                2.596195083723413f,
                1.5339159641533686f,
                4.128691418145582f,
                2.8556901801591392f,
                -6.160436355512531f,
                -4.860465006062838f,
                2.357899080012385f,
                -0.32523974178310855f
            ),
            arrayOf(
                6.262093208344162f,
                3.600737918539337f,
                0.2087665120406197f,
                -2.736923272298697f,
                -2.9157057991527573f,
                -3.208480889546755f,
                -0.6483039277190044f,
                2.0365364938850115f,
                -3.060195081684604f,
                0.00478453253644736f
            ),
            arrayOf(
                4.485330292808015f,
                3.5619209045067923f,
                0.6311076032438854f,
                -3.3306244730820787f,
                2.1167759482970094f,
                -4.537688380020497f,
                0.18249308212651166f,
                0.14226344857789563f,
                -2.9632951410994544f,
                0.049585126593915715f
            )
        )
    }
}