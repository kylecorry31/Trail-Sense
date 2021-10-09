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
                -4.739066290398845f,
                -0.9377083474677119f,
                -1.0455718188338203f,
                2.342445774439856f,
                2.1006842112776165f,
                6.729532673507037f,
                1.6234042743810189f,
                -8.738993937682995f,
                0.8149371004797953f,
                1.9433081298109702f
            ),
            arrayOf(
                0.04035776353149581f,
                -4.177490323111603f,
                -0.7868945596576171f,
                0.6981643450488475f,
                -2.9978778166409756f,
                -0.48453161565999797f,
                0.9524186439825271f,
                6.611231692198447f,
                0.4152381044854649f,
                -0.4809089472371988f
            ),
            arrayOf(
                6.020707118665337f,
                0.18220445629668663f,
                -0.7892373217463958f,
                0.4452601522815624f,
                -1.309967757658771f,
                -0.6144999347964095f,
                -0.580219353724754f,
                0.25798920017773896f,
                0.6419342996119534f,
                -3.93504426515124f
            ),
            arrayOf(
                -0.09683089376697175f,
                0.45135520555806935f,
                -0.6043607770344196f,
                -0.16683723347883817f,
                0.29986487312182103f,
                0.2177187941211246f,
                -0.6004140041242181f,
                1.168184846066484f,
                -0.8417588468849222f,
                -0.15200767252934116f
            ),
            arrayOf(
                -2.158532627178212f,
                -1.2061683993677363f,
                -0.531317694064838f,
                0.019352119514534596f,
                -0.871241362048602f,
                0.2663474760932422f,
                0.42645112391984213f,
                3.2167901960867495f,
                -0.5856028629573669f,
                1.2016754572788457f
            ),
            arrayOf(
                -1.5135846221286369f,
                -0.20236109418511236f,
                -0.6215671794691096f,
                -0.23503344265284312f,
                0.0018706283808883964f,
                0.27639430529627684f,
                0.10705303570261845f,
                2.0219500389956897f,
                -0.1939676226021624f,
                0.1669722956821958f
            ),
            arrayOf(
                5.560198934073339f,
                0.5928688073500146f,
                -0.03686582800605944f,
                -0.6017342579786511f,
                -4.298805871236281f,
                1.8127114945955713f,
                -4.105918609600367f,
                -1.8645057285839164f,
                4.190875881954374f,
                -1.211273930286561f
            ),
            arrayOf(
                -1.640754914515906f,
                3.232747961308219f,
                -0.755055710671156f,
                -0.7421568065005931f,
                3.0895121166460027f,
                -2.9493766035861886f,
                1.381138526801836f,
                -0.3874751901890541f,
                -1.7210710584182098f,
                0.2711317626907431f
            ),
            arrayOf(
                -4.5727395717010895f,
                6.337309573786085f,
                -0.30275512630864043f,
                -1.7546445648373725f,
                5.087523108780741f,
                -7.853579009152081f,
                -2.18493018548137f,
                5.534537600065052f,
                1.9994999996473604f,
                -2.1351613340846067f
            ),
            arrayOf(
                1.8591736558856917f,
                -0.5745659936859978f,
                -0.3855383237275483f,
                0.12025193024281575f,
                -5.063951684917722f,
                0.2991330401291569f,
                0.02325431395283883f,
                0.3097971435151556f,
                0.48417711031418154f,
                2.7497137751749574f
            ),
            arrayOf(
                -1.7094392041325528f,
                2.9177246429614097f,
                -0.05665678763009717f,
                -0.3363611327291898f,
                -0.3066441689630647f,
                -3.9017183592874547f,
                1.9185931654845243f,
                -0.033404631854424965f,
                0.09329156881594848f,
                1.5170357203697746f
            ),
            arrayOf(
                1.529416265489981f,
                -1.1177367366175093f,
                -0.3670298172353547f,
                -3.1325815825050394f,
                3.800711114996964f,
                -3.656819145491161f,
                3.9997793343125396f,
                0.49398947434844354f,
                -5.815433587339377f,
                4.660372813434851f
            ),
            arrayOf(
                1.3298029786081385f,
                2.269256896819843f,
                -1.324224175555893f,
                -0.2012228386012013f,
                0.45260915514729927f,
                0.2087350451357091f,
                -0.9489447427128306f,
                0.06988115122003043f,
                -0.9147050316354669f,
                -1.127838261061211f
            )
        )
    }

}