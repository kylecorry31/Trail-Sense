package com.kylecorry.trail_sense.weather.domain.clouds

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.glcm
import com.kylecorry.andromeda.core.bitmap.ColorChannel
import com.kylecorry.sol.math.SolMath.map
import com.kylecorry.sol.math.SolMath.power
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.math.classifiers.LogisticRegressionClassifier
import com.kylecorry.sol.math.statistics.GLCMService
import com.kylecorry.sol.math.statistics.StatisticsService
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.shared.specifications.FalseSpecification

/**
 * A cloud classifier using the method outlined in: doi:10.5194/amt-3-557-2010
 */
class AMTCloudClassifier(
    private val skyDetectionSensitivity: Int,
    private val obstacleRemovalSensitivity: Int
) : ICloudClassifier {

    private val statistics = StatisticsService()

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    override suspend fun classify(
        bitmap: Bitmap,
        setPixel: (x: Int, y: Int, classification: SkyPixelClassification) -> Unit
    ): List<ClassificationResult<CloudGenus>> {
        var skyPixels = 0
        var cloudPixels = 0

        var redMean = 0.0
        var greenMean = 0.0
        var blueMean = 0.0

        val cloudBluePixels = mutableListOf<Float>()

        val isSky = NRBRIsSkySpecification(skyDetectionSensitivity / 200f)

        val isObstacle = SaturationIsObstacleSpecification(1 - obstacleRemovalSensitivity / 100f)
            .or(BrightnessIsObstacleSpecification(0.75f * obstacleRemovalSensitivity.toFloat()))
            .or(if (obstacleRemovalSensitivity > 0) IsSunSpecification() else FalseSpecification())

        val cloudBitmap = bitmap.copy(bitmap.config, true)

        for (w in 0 until bitmap.width) {
            for (h in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(w, h)

                when {
                    isSky.isSatisfiedBy(pixel) -> {
                        skyPixels++
                        setPixel(w, h, SkyPixelClassification.Sky)
                        cloudBitmap.setPixel(w, h, Color.TRANSPARENT)
                    }
                    isObstacle.isSatisfiedBy(pixel) -> {
                        setPixel(w, h, SkyPixelClassification.Obstacle)
                        cloudBitmap.setPixel(w, h, Color.TRANSPARENT)
                    }
                    else -> {
                        cloudPixels++
                        redMean += Color.red(pixel)
                        blueMean += Color.blue(pixel)
                        greenMean += Color.green(pixel)
                        cloudBluePixels.add(Color.blue(pixel).toFloat())
                        setPixel(w, h, SkyPixelClassification.Cloud)
                    }
                }
            }
        }

        val glcm = cloudBitmap.glcm(1 to 1, ColorChannel.Blue, true)
        cloudBitmap.recycle()
        val texture = GLCMService().features(glcm)

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

        val blueStdev = statistics.stdev(cloudBluePixels, mean = blueMean.toFloat())
        val blueSkewness =
            map(
                statistics.skewness(cloudBluePixels, blueMean.toFloat(), blueStdev),
                -3f,
                3f,
                0f,
                1f
            )

        cloudBluePixels.clear()

        // TODO: If no clouds or < 10% cloudiness return no clouds
        if (cover < 0.1) {
            return emptyList()
        }

        val features = CloudImageFeatures(
            cover,
            texture.contrast / 255f,
            texture.energy * 100,
            texture.entropy / 16f,
            texture.homogeneity,
            (redMean / 255).toFloat(),
            (blueMean / 255).toFloat(),
            percentDifference(redMean, greenMean),
            percentDifference(redMean, blueMean),
            percentDifference(greenMean, blueMean),
            blueStdev / 255f,
            blueSkewness
        )

        val classifier = LogisticRegressionClassifier(weights)

        val prediction = classifier.classify(
            listOf(
                features.cover,
                features.redMean,
                features.blueMean,
                features.redGreenDiff,
                features.redBlueDiff,
                features.greenBlueDiff,
                features.energy,
                features.entropy,
                features.contrast,
                features.homogeneity,
                features.blueStdev,
                features.blueSkewness,
                1f
            )
        )

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

        val result = prediction.mapIndexed { index, confidence ->
            ClassificationResult(cloudMap[index], confidence)
        }.sortedByDescending { it.confidence }

        logFeatures(features)

        return result
    }

    /**
     * Logs an observation to the console in CSV training format
     */
    private fun logFeatures(features: CloudImageFeatures) {
        val values = listOf(
            features.cover,
            features.redMean,
            features.blueMean,
            features.redGreenDiff,
            features.redBlueDiff,
            features.greenBlueDiff,
            features.energy,
            features.entropy,
            features.contrast,
            features.homogeneity,
            features.blueStdev,
            features.blueSkewness
        )

        Log.d("CloudFeatures", values.joinToString(",") { it.roundPlaces(2).toString() })
    }

    private fun percentDifference(color1: Double, color2: Double): Float {
        return map((color1 - color2).toFloat(), -255f, 255f, 0f, 1f)
    }

    fun StatisticsService.skewness(
        values: List<Float>,
        mean: Float? = null,
        stdev: Float? = null
    ): Float {
        val average = mean ?: values.average().toFloat()
        val deviation = stdev ?: stdev(values, mean = average)

        return values.sumOf {
            power((it - average) / deviation.toDouble(), 3)
        }.toFloat() / values.size
    }


    private fun average(@ColorInt color: Int): Float {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        return (r + g + b).toFloat() / 3f
    }

    private data class CloudImageFeatures(
        val cover: Float,
        val contrast: Float,
        val energy: Float,
        val entropy: Float,
        val homogeneity: Float,
        val redMean: Float,
        val blueMean: Float,
        val redGreenDiff: Float,
        val redBlueDiff: Float,
        val greenBlueDiff: Float,
        val blueStdev: Float,
        val blueSkewness: Float
    )

    companion object {
        private val weights = arrayOf(
            arrayOf(
                -5.091728016842531f,
                -1.3859960153302038f,
                -0.971381509196803f,
                2.3784017200450074f,
                5.381021660953017f,
                6.207354145431935f,
                1.2018646371796866f,
                -10.179989672148075f,
                0.6492395919700245f,
                1.9810944335182359f
            ),
            arrayOf(
                0.45324172381275346f,
                -4.125249492027855f,
                -0.657884578049453f,
                0.5295499737095317f,
                -3.1358227185041923f,
                -1.0779308791908537f,
                1.7822136118156298f,
                6.6309852828279015f,
                0.37084509422249035f,
                -0.5758315386922366f
            ),
            arrayOf(
                5.917657044666159f,
                0.5574766488468674f,
                -0.8127879016109792f,
                0.5068907594978722f,
                -3.2039105966569332f,
                -0.3904863815457096f,
                -0.7588123263487038f,
                1.4653277035920875f,
                0.6754456211890121f,
                -3.7131610317763246f
            ),
            arrayOf(
                -0.3118029152574676f,
                0.5435717877636422f,
                -0.6447427315657004f,
                -0.040346304225674076f,
                -0.2964064475131486f,
                0.2943210480740899f,
                -0.1881481668581464f,
                1.2822980790246978f,
                -0.5768467370464774f,
                -0.031302147872136976f
            ),
            arrayOf(
                -1.9570104706291482f,
                -1.144432536714633f,
                -0.449548559017368f,
                0.22140156518626014f,
                -0.3572971473937534f,
                -0.18123954091759675f,
                0.874718183073113f,
                2.936757511359865f,
                -0.45165589623077285f,
                1.0191169171315468f
            ),
            arrayOf(
                -1.1974190801612608f,
                -0.10114515671883154f,
                -0.6237844601249954f,
                -0.30233070829436315f,
                -0.6113423536985313f,
                -0.18385419878697573f,
                0.5905636674292157f,
                2.208779837518751f,
                -0.15344113450499344f,
                0.28066546921028973f
            ),
            arrayOf(
                5.186238015131057f,
                0.29193806034430786f,
                -0.2203099243584984f,
                -0.6197052736287922f,
                -3.7286298017376924f,
                2.4262326962383507f,
                -4.483657605522327f,
                -2.0778651688634895f,
                4.243553382865873f,
                -1.1340377860556892f
            ),
            arrayOf(
                -1.1657969339144378f,
                3.311296333006599f,
                -0.6973793753337314f,
                -0.5132197758204726f,
                2.09364771124759f,
                -2.696024212174635f,
                1.7139849073659363f,
                -0.17432525702350998f,
                -1.8795536242637128f,
                0.27715636081812106f
            ),
            arrayOf(
                -4.622817427649341f,
                5.98412872203523f,
                -0.31679359205929114f,
                -2.0782866031812905f,
                6.458813847543923f,
                -7.587955379003534f,
                -1.7039494692384687f,
                4.718023640195591f,
                1.8257416087866023f,
                -2.625564206661612f
            ),
            arrayOf(
                1.789127838550626f,
                -0.665052010809697f,
                -0.416469021247002f,
                0.528580614224893f,
                -4.5476858222582415f,
                1.5154696887287924f,
                -1.9290763640696655f,
                0.19671519445521465f,
                0.6857719643797011f,
                2.9291450123239526f
            ),
            arrayOf(
                -1.6299881266138851f,
                2.6614936660024693f,
                -0.09716281012092899f,
                -0.10621584777921494f,
                -0.12221583770462321f,
                -2.6733722443613006f,
                0.665059589347103f,
                -0.48239268958288023f,
                -0.08941945526936951f,
                1.6970552771835334f
            ),
            arrayOf(
                1.0175219453310496f,
                -1.3265625662104237f,
                -0.40410904009666804f,
                -3.079747659307396f,
                3.526876712737525f,
                -3.2827105111550705f,
                3.9359216216779602f,
                0.7883705625332859f,
                -5.752990658341734f,
                4.421787935044064f
            ),
            arrayOf(
                1.6402884817509291f,
                2.5254754907190873f,
                -1.2378773902961895f,
                -0.29498330806796347f,
                -0.874122714517492f,
                0.19677706592553193f,
                -0.9045243432131316f,
                0.6711558424215439f,
                -0.7187628727576632f,
                -1.0145147439533682f
            )
        )
    }

}