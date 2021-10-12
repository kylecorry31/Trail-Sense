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
                -7.415653220548138f,
                -2.015171668317727f,
                5.154792111690049f,
                -1.1161058125093026f,
                0.3883625196223636f,
                7.102292473072352f,
                2.378677341425688f,
                -9.031641435385612f,
                2.467741519000359f,
                1.9917907479425636f
            ),
            arrayOf(
                -3.2725081130300007f,
                -4.533751148793247f,
                -8.378042252622672f,
                -0.7745475977161423f,
                0.26951182489829356f,
                0.9951473040454315f,
                3.1952254851844533f,
                9.757931850514424f,
                1.9098710434895354f,
                0.3934076059619916f
            ),
            arrayOf(
                8.16580195322183f,
                1.5417413381235532f,
                -2.687149593212015f,
                -0.9012958082308846f,
                -0.5320579912356366f,
                0.04514848230669667f,
                -1.1528885533827384f,
                -0.25240793445279963f,
                0.1980844069722964f,
                -4.040011518234335f
            ),
            arrayOf(
                -0.14258609606104558f,
                0.3438504997472548f,
                -0.392940289538999f,
                -0.6311889597123956f,
                0.5851890905230008f,
                0.2090927858817756f,
                -0.5527596170976258f,
                0.998124487989948f,
                -0.2415315659063132f,
                0.010744423367043383f
            ),
            arrayOf(
                -3.8950338031551097f,
                -1.630352716626351f,
                -2.5985404232843865f,
                -0.6252350283156005f,
                0.3975966001837013f,
                0.8321785422344196f,
                1.2205931914518973f,
                4.469322563902477f,
                -0.04646394579704084f,
                1.5366778617576884f
            ),
            arrayOf(
                -2.211064179568435f,
                -0.37236108683067226f,
                -1.9285475798058314f,
                -0.6150132559508327f,
                0.41343963262629435f,
                0.330087865702641f,
                0.5527055443577489f,
                2.8023643880406763f,
                0.08775118704160968f,
                0.692811559833542f
            ),
            arrayOf(
                5.83717682240866f,
                1.2535177508759494f,
                -0.7653630266640511f,
                -0.26311212978978266f,
                -5.935132658720617f,
                -2.0394735650305087f,
                -4.639580172093202f,
                -1.6909720935538306f,
                9.882045612878368f,
                -1.45266357441952f
            ),
            arrayOf(
                -0.13360866178611255f,
                4.0034740218748f,
                0.016027451448004923f,
                -0.8338300039902045f,
                3.9325695141577963f,
                -2.2134738986061255f,
                0.5758161114356483f,
                -1.1760545449761572f,
                -4.3239391968948f,
                0.16909658355224744f
            ),
            arrayOf(
                -3.5395415874739924f,
                6.640719142302436f,
                -0.7404164199625609f,
                -0.3167252053280461f,
                3.8287293653208994f,
                -6.608233012857751f,
                -1.321535887819229f,
                7.043720222640775f,
                -3.8183406361131973f,
                -1.6407787752330911f
            ),
            arrayOf(
                2.709311006844781f,
                -1.8017995505965898f,
                5.559680690186159f,
                -0.44891716740624815f,
                -6.115753800309066f,
                0.27925383471334947f,
                -0.6517995881772698f,
                -0.05391601596209507f,
                -1.4772856560743322f,
                2.427032374015031f
            ),
            arrayOf(
                -1.7225329495363508f,
                1.7610463287580085f,
                2.368145782915547f,
                -0.1185999636900175f,
                0.6818708483900071f,
                -2.85344765244721f,
                1.104068719060408f,
                -0.3414254277731883f,
                -2.487254366815338f,
                1.5201952857015564f
            ),
            arrayOf(
                1.584424207650613f,
                -2.795336638313376f,
                4.862534276902422f,
                -0.5391051420739865f,
                1.0222525823569897f,
                -7.882918867398794f,
                2.438877545418869f,
                -1.9378756554922825f,
                0.2041406119064751f,
                3.4015212079349624f
            ),
            arrayOf(
                3.363117257152492f,
                3.2690444495729434f,
                0.6290868683424558f,
                -1.1937516068210772f,
                0.2707581590203315f,
                -0.01667918826735455f,
                -1.9216012581914668f,
                -0.9994351083678839f,
                -1.5429833202303411f,
                -1.7354010002976918f
            )
        )
    }

}