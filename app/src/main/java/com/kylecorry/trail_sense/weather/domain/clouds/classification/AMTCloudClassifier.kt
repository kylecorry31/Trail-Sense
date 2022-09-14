package com.kylecorry.trail_sense.weather.domain.clouds.classification

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.getChannel
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.glcm
import com.kylecorry.andromeda.core.bitmap.ColorChannel
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.map
import com.kylecorry.sol.math.algebra.createMatrix
import com.kylecorry.sol.math.classifiers.LogisticRegressionClassifier
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.math.statistics.Texture
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.shared.ClassificationResult
import com.kylecorry.trail_sense.weather.domain.clouds.mask.ICloudPixelClassifier
import com.kylecorry.trail_sense.weather.domain.clouds.mask.SkyPixelClassification
import kotlin.math.roundToInt

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
                        // Do nothing
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
                ColorChannel.Blue,
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
            Statistics.median(textures.map { it.entropy / 16f }),
            Statistics.median(textures.map { it.contrast / levels.toFloat() }),
            Statistics.median(textures.map { it.verticalMean / levels.toFloat() }),
            Statistics.median(textures.map { it.correlation }),
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
                -4.8890135143776785f,
                -4.86067579758433f,
                0.6496566261447395f,
                6.189094474308713f,
                -4.49221248576373f,
                9.217741694338727f,
                4.328311402501023f,
                -11.439505036575826f,
                7.525365592313418f,
                -2.0894330280650233f
            ),
            arrayOf(
                -13.067279937853305f,
                -1.727532994954166f,
                -3.179860972961632f,
                2.672091281591219f,
                0.8402879000505321f,
                3.743056520642552f,
                -0.07268128021855019f,
                9.620963032178897f,
                1.1814832319829f,
                0.14155840583401008f
            ),
            arrayOf(
                9.451529208392932f,
                1.6349773030585206f,
                2.247829140771352f,
                -0.6882934368397159f,
                0.9124205704743994f,
                0.15845079449974458f,
                -8.943848344707577f,
                -5.0628528540423225f,
                0.6493928820113739f,
                -0.33840482378936254f
            ),
            arrayOf(
                1.5398322263853312f,
                2.443923457771001f,
                0.14979395258288056f,
                -0.8288368105020324f,
                -1.1096920244220965f,
                -1.6375951422617385f,
                -1.8107822396943023f,
                1.06527386396407f,
                0.1652388420184461f,
                0.14074625064127255f
            ),
            arrayOf(
                -6.992460320283093f,
                1.398189500397681f,
                -1.6702190646681068f,
                0.49781350093393867f,
                0.14878787562227208f,
                -0.7836038085131055f,
                1.9468168185474857f,
                5.640569089587589f,
                0.12328792343627595f,
                0.31289813468583183f
            ),
            arrayOf(
                -4.495281228938655f,
                2.111416732188496f,
                -0.7197048730635448f,
                0.2895367046133483f,
                1.2210026112671148f,
                -1.8954683680944202f,
                1.1961043053692426f,
                2.638530840278272f,
                0.11696872702883662f,
                0.12952382941722465f
            ),
            arrayOf(
                -2.378504062748752f,
                5.16368529037409f,
                -1.4421524398734296f,
                -2.87353783619913f,
                5.470393652843714f,
                -6.248800642069856f,
                4.8173804673980865f,
                0.582279819471783f,
                -3.39501585849374f,
                0.47162836315960216f
            ),
            arrayOf(
                -0.47806708132116776f,
                1.2072265757036962f,
                -0.21202636898678337f,
                -0.3033477116688911f,
                1.0283431630641509f,
                -0.8108241466188087f,
                0.20934685982995946f,
                -0.43786322231590824f,
                -0.2533283879249874f,
                -0.0525657981890969f
            ),
            arrayOf(
                1.8226423004834906f,
                -5.724456835502467f,
                3.028268734024902f,
                1.0377899492775704f,
                1.6143635093683089f,
                3.41794093767296f,
                -3.8519396579385208f,
                -2.1446240308649016f,
                1.9302482551800766f,
                -0.8420599824917269f
            ),
            arrayOf(
                0.04818627518619918f,
                -1.5687078505760832f,
                -4.214610472948905f,
                -5.679910817376066f,
                2.53402043805123f,
                -4.394108008570153f,
                12.263693284553076f,
                10.526340632765685f,
                -10.354311859070377f,
                0.5300933484945843f
            ),
            arrayOf(
                8.499331894904152f,
                6.123214507113178f,
                1.8469230551855653f,
                -2.2608757483364377f,
                0.4036479254096566f,
                -5.399336245503867f,
                -5.198807747495122f,
                -3.700342330485511f,
                -0.5231923496388189f,
                0.13501716989779736f
            )
        )
    }
}