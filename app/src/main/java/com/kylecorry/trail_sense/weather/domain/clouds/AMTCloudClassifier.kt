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
                -6.150633226860857f,
                -1.4546950535282266f,
                3.7465742567258737f,
                -0.9919757909814592f,
                -0.653871913136005f,
                7.239296899417418f,
                1.8442660404219802f,
                -8.55375899262476f,
                3.01070029102783f,
                1.8297494799758443f
            ),
            arrayOf(
                -0.8979637749637035f,
                -3.253429852414551f,
                -5.955229593964653f,
                -0.6936099095366364f,
                -1.4286555230860865f,
                0.20394079272746066f,
                2.6739367739416537f,
                7.396887980274817f,
                1.7302663215173077f,
                0.0540693907575464f
            ),
            arrayOf(
                6.474168991705344f,
                0.9364714744720791f,
                -3.111155896034714f,
                -0.9568058238711962f,
                0.44659507177700986f,
                -0.08287815077974489f,
                -0.7145708488707141f,
                0.2667709959571418f,
                -0.0018203162202804967f,
                -3.6128220892953986f
            ),
            arrayOf(
                -0.2155990608435425f,
                0.401060568856279f,
                0.013801190265542333f,
                -0.5093390994102831f,
                0.7108541440952308f,
                0.08748628493895541f,
                -0.7965408037386952f,
                1.1640866883689789f,
                -0.18519455052919637f,
                -0.21781472274840924f
            ),
            arrayOf(
                -2.5088696805659327f,
                -1.0512961719002127f,
                -1.5374563243922248f,
                -0.6689961217764941f,
                -0.5482032024537745f,
                0.524099152916546f,
                1.0609484881065006f,
                3.454667614736126f,
                -0.044033311351536215f,
                1.1727929835258475f
            ),
            arrayOf(
                -1.5821845739736582f,
                -0.2220275375181485f,
                -1.0709697579408881f,
                -0.6515601629353136f,
                -0.21519075293513076f,
                0.10927031462130242f,
                0.22337669898373122f,
                2.289366247596802f,
                -0.29515985482309376f,
                0.33050727206729374f
            ),
            arrayOf(
                6.564627642905853f,
                1.0099476405743042f,
                -2.4224171650428477f,
                -0.23154751568642465f,
                -5.878838009755324f,
                -1.4317842689602975f,
                -4.1739850604699f,
                -1.7275104176835356f,
                9.982996926202727f,
                -1.4128932279643305f
            ),
            arrayOf(
                -1.5253917957824403f,
                3.3878250299429644f,
                0.7625040472245935f,
                -0.7864374839802197f,
                4.755825897190604f,
                -2.028471055253469f,
                0.7141794017894137f,
                -0.5294853157257623f,
                -4.517756068844238f,
                0.11510569694116456f
            ),
            arrayOf(
                -3.972430269692824f,
                6.708253166820467f,
                1.1594637133653356f,
                -0.32449241660485034f,
                3.357263384677142f,
                -6.61369690599349f,
                -1.4904427101864812f,
                6.868933282635947f,
                -3.836371082368264f,
                -1.8477890599492142f
            ),
            arrayOf(
                3.103636014940284f,
                -1.1244704445686275f,
                2.426006286236788f,
                -0.5168142466773249f,
                -6.136153814747022f,
                1.1519403602053795f,
                -0.28071534835137707f,
                0.1308797189946245f,
                -1.5073756847889228f,
                2.7477537721385095f
            ),
            arrayOf(
                -1.5077062709917162f,
                2.411609934165702f,
                2.637680962602593f,
                -0.15299001649143928f,
                0.35753009629569404f,
                -3.2872322546231665f,
                1.2045080530968375f,
                -0.4550913732443071f,
                -2.5485635460627987f,
                1.4219588696195f
            ),
            arrayOf(
                0.9333974618850923f,
                -3.333797259883129f,
                3.813208594086494f,
                -0.5880040147876099f,
                2.190250183912224f,
                -7.987155196557533f,
                2.4549537305611544f,
                -1.7211203786348277f,
                0.3554775199820286f,
                3.5528096499587525f
            ),
            arrayOf(
                1.8936694437367854f,
                2.264666283850167f,
                0.5697920557389392f,
                -1.302422327727115f,
                1.2846550052445527f,
                0.07837009154679916f,
                -1.499308794034164f,
                -0.28418900117935664f,
                -1.557425328367359f,
                -1.5009076778284804f
            )
        )
    }

}