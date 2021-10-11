package com.kylecorry.trail_sense.weather.domain.clouds

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
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

        if (cover < 0.05) {
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
                -4.830253690017165f,
                -0.7314572214061759f,
                -0.9767724700735939f,
                -0.8864813097176517f,
                2.2287699493019932f,
                7.291280975679492f,
                1.9569733155402635f,
                -8.444707163056114f,
                2.2276128023903516f,
                1.8369334137016329f
            ),
            arrayOf(
                0.11721458165497925f,
                -3.96506065981306f,
                -0.5848672304305625f,
                -0.743549684326052f,
                -3.1382404324137f,
                -0.13507582141582455f,
                0.8938293282954791f,
                6.5545312037216465f,
                1.3937164959973483f,
                -0.1099215563396789f
            ),
            arrayOf(
                6.0024004105822195f,
                0.5592675639856114f,
                -0.8949260749720973f,
                -0.9091017039251117f,
                -1.6482262131657912f,
                0.18621630631184585f,
                -0.2676089686475049f,
                0.5584173982681597f,
                0.2258071013813277f,
                -3.6262701943200457f
            ),
            arrayOf(
                -0.15043745609377304f,
                0.45735979961815837f,
                -0.649267162020244f,
                -0.4400891993240692f,
                0.37519089724735644f,
                0.13175285127254793f,
                -0.45508549262436204f,
                1.2634532787896298f,
                -0.11752142583584045f,
                -0.2410528364929683f
            ),
            arrayOf(
                -2.1052902218813907f,
                -1.2949241990940978f,
                -0.4712360633982133f,
                -0.6411873930955727f,
                -0.6736244576714202f,
                0.13238028417515899f,
                0.323927713090178f,
                3.3019138885362316f,
                -0.028239897009287175f,
                0.9922272147429484f
            ),
            arrayOf(
                -1.066215163502865f,
                -0.19851657787357424f,
                -0.46573502524212773f,
                -0.598517885545457f,
                -0.09694977001845483f,
                0.03268007924942288f,
                0.01566086724899625f,
                2.170627086532773f,
                -0.1542756441793437f,
                0.5504064287630488f
            ),
            arrayOf(
                4.659574264244802f,
                0.9691845989769184f,
                -0.1879177997045736f,
                -0.14702920733811073f,
                -4.615452672147987f,
                -1.876435934702037f,
                -5.3628466037023435f,
                -1.772778522247127f,
                9.906497179540123f,
                -1.3869001933386038f
            ),
            arrayOf(
                -1.1872827629047746f,
                3.4002036629328956f,
                -0.8287180360194253f,
                -0.8090424795360275f,
                3.0645907785732396f,
                -1.9458408628498653f,
                2.1190311346053337f,
                -0.5273915145593338f,
                -4.0092447363786f,
                0.38911997988893887f
            ),
            arrayOf(
                -3.6171409304348057f,
                7.084362528173136f,
                -0.2971798761840997f,
                -0.34924407591126094f,
                5.606423260900952f,
                -6.510373664902239f,
                -2.531401891710183f,
                6.184441510090818f,
                -3.820687971349964f,
                -1.7878257671806486f
            ),
            arrayOf(
                2.2220343524048687f,
                -0.5091060723977148f,
                -0.4924439445377298f,
                -0.4983526829275998f,
                -5.060417458437602f,
                1.4341224625475817f,
                0.3936723448156956f,
                0.5876111379050166f,
                -1.1117166808217263f,
                2.839396140784811f
            ),
            arrayOf(
                -1.3021790861209335f,
                2.8569386467634534f,
                -0.026817170360135815f,
                0.02981323833515095f,
                -0.5365036638151948f,
                -2.707955824034924f,
                2.3681423070112015f,
                -0.01321220554432749f,
                -2.4681965034610314f,
                1.7345478808344637f
            ),
            arrayOf(
                0.244443456745537f,
                -2.461865749878848f,
                -0.43792668707206744f,
                -0.4029930902675197f,
                4.16521338793401f,
                -7.236294473500507f,
                2.7130693981832574f,
                -0.39615762585317404f,
                0.7221879240767639f,
                4.003718013072389f
            ),
            arrayOf(
                1.5412015900584508f,
                2.2412919209307423f,
                -1.0698759972970462f,
                -1.0289988228978917f,
                0.8022094676307079f,
                0.39670183814319293f,
                -0.8247464214752047f,
                0.14415976233933356f,
                -1.0333583685420937f,
                -1.2091962802804828f
            )
        )
    }

}