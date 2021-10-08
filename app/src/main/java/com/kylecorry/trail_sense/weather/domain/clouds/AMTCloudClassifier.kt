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
                -4.6062652714553805f,
                -0.9850748500566094f,
                -1.0649698852198675f,
                2.3522693027074357f,
                1.9300064345904921f,
                6.64479405391597f,
                1.2824712166190084f,
                -8.733981513958824f,
                0.7911377808514797f,
                2.0215429123913777f
            ),
            arrayOf(
                0.11249559783790396f,
                -4.271922713134001f,
                -0.5958430972335481f,
                0.736083781723012f,
                -3.0358694419995094f,
                -1.1899204470183888f,
                1.848290813711072f,
                6.742202495051301f,
                0.39836946752218444f,
                -0.4688407378709678f
            ),
            arrayOf(
                5.541894936108259f,
                0.35706823030901f,
                -0.9286020465685308f,
                0.40140042428094125f,
                -1.210152169774876f,
                -0.44105672360663606f,
                -0.8715528460413771f,
                0.13908386903482434f,
                0.6119308328941481f,
                -3.8441271371946164f
            ),
            arrayOf(
                -0.08827774155861838f,
                0.26839742259603805f,
                -0.5508600862843601f,
                -0.17122802062526854f,
                0.4022871003889591f,
                0.08376112047489014f,
                -0.269763951994719f,
                1.0466636119877428f,
                -0.6006237019759417f,
                -0.18441391503192134f
            ),
            arrayOf(
                -1.8841661270450782f,
                -1.1577801739484943f,
                -0.4785913584460891f,
                0.03269751690920955f,
                -0.6399237517650576f,
                -0.2691452583031583f,
                1.0464093239467918f,
                3.255064860777081f,
                -0.4438321744656447f,
                1.174282010583115f
            ),
            arrayOf(
                -1.2434257713372883f,
                -0.09960951884217661f,
                -0.6114678985984026f,
                -0.29675874288068077f,
                -0.1419756142005675f,
                -0.17034761180909241f,
                0.4605720075683326f,
                2.299468764094983f,
                -0.2671957239797328f,
                0.3458484890092271f
            ),
            arrayOf(
                5.264701072311815f,
                0.35257626194051456f,
                -0.09565327511534738f,
                -0.5591058133433516f,
                -4.262715000818147f,
                2.3261330618458786f,
                -4.370741367657614f,
                -1.9502053567554747f,
                4.317092205713096f,
                -1.112471641110642f
            ),
            arrayOf(
                -1.3424231408305267f,
                3.4177034890801257f,
                -0.7224813609746346f,
                -0.6436786080314137f,
                3.314670095801176f,
                -2.9806612316000574f,
                1.416218145885441f,
                -0.4162185689066776f,
                -1.6434139054887746f,
                0.16146522955517473f
            ),
            arrayOf(
                -4.566273372007293f,
                6.380785727929718f,
                -0.3288814029195939f,
                -2.1776064536035973f,
                5.060712965466385f,
                -7.853846209380989f,
                -1.3879377429883812f,
                5.536554442170306f,
                1.9484380799666798f,
                -2.400539601795299f
            ),
            arrayOf(
                2.0653739724378526f,
                -0.3901177845809842f,
                -0.4458062685466525f,
                0.42406646516442575f,
                -4.980513162140775f,
                1.676625652206715f,
                -1.8656481197528811f,
                0.6804299122556982f,
                0.47056326250219055f,
                3.1410897978810106f
            ),
            arrayOf(
                -1.4937128662698875f,
                2.549195328885858f,
                -0.1363932278211734f,
                0.003503534876101832f,
                -0.2961191085000308f,
                -2.912880012462508f,
                0.840017518039808f,
                0.04850106181348156f,
                -0.2647431200736602f,
                1.7291198146317324f
            ),
            arrayOf(
                1.2447606605716295f,
                -1.1505438552138376f,
                -0.2616516471283215f,
                -2.968119544781073f,
                3.8129727277482726f,
                -3.3235170941203904f,
                3.968118353983446f,
                0.6068324955337906f,
                -5.677554997442136f,
                4.3941877560393205f
            ),
            arrayOf(
                1.2879979880999424f,
                2.183457962436054f,
                -1.230288632425639f,
                -0.20984228169418218f,
                0.5668659087322772f,
                0.37763938264989516f,
                -0.9880181878073608f,
                -0.03201585230064502f,
                -0.9300237636447831f,
                -1.2155144090621133f
            )
        )
    }

}