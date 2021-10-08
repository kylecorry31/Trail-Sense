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
                -5.795895936177978f,
                -1.6406653573807533f,
                -0.9373160908045084f,
                2.25944385810141f,
                6.011023586880006f,
                6.368185122437619f,
                1.0236183845053926f,
                -10.24715934833954f,
                0.8093817119683255f,
                2.038857552989557f
            ),
            arrayOf(
                0.3147913671405923f,
                -3.9357796653657595f,
                -0.7320621531008585f,
                0.7776587526779141f,
                -4.374025577309889f,
                -1.0494964179325703f,
                2.0912346606600702f,
                7.068045424061466f,
                0.4832579525286845f,
                -0.5459358839566782f
            ),
            arrayOf(
                6.154224539335674f,
                0.7741960250372117f,
                -1.012784539895266f,
                0.587459748491915f,
                -3.6600297530392543f,
                -0.5100469307482098f,
                -0.8196285568979453f,
                1.3156581739589919f,
                0.66925402066193f,
                -3.7743745428744524f
            ),
            arrayOf(
                -0.10339355090389832f,
                0.3831682205404226f,
                -0.6195459388378252f,
                -0.026409783757201752f,
                0.059386291569649884f,
                0.09281747363996642f,
                -0.2392504591070954f,
                1.289847904852742f,
                -0.701081069837663f,
                -0.11599078261784318f
            ),
            arrayOf(
                -1.9185732647176559f,
                -1.1977286910621068f,
                -0.49059227280339873f,
                0.2322890943093176f,
                -0.6026522230888864f,
                -0.23054688484791208f,
                0.8376671187477353f,
                2.944442660091545f,
                -0.6305888444512204f,
                0.9149930953489454f
            ),
            arrayOf(
                -0.871040214044197f,
                -0.23789708965913964f,
                -0.4777909893314037f,
                -0.3104727020961926f,
                -0.47636242594750766f,
                -0.015864124810281387f,
                0.5143716081639426f,
                2.0124428320435697f,
                -0.26922834585245803f,
                0.5193318399563291f
            ),
            arrayOf(
                5.638137510821945f,
                0.37406346815670133f,
                -0.011647177763251963f,
                -0.49330604239320236f,
                -4.651188791910257f,
                2.5239374187002763f,
                -4.182565331568588f,
                -2.213509572854763f,
                4.326264078251027f,
                -0.9782206462564925f
            ),
            arrayOf(
                -1.2591546626758339f,
                3.3750478047078887f,
                -0.7305892362894365f,
                -0.6329969688842572f,
                2.5976749214145785f,
                -3.0843694903134744f,
                1.32681180088886f,
                -0.18265512914944548f,
                -1.7512089356781706f,
                0.2550601296130421f
            ),
            arrayOf(
                -4.85548841938685f,
                6.394798749124713f,
                -0.3558497205925493f,
                -2.1349904360076835f,
                6.144046678572407f,
                -7.611996268577128f,
                -1.4384681494120954f,
                4.88161898582195f,
                1.9461831915863186f,
                -2.7179102887277686f
            ),
            arrayOf(
                2.377847140717189f,
                -0.5849969057237949f,
                -0.5857243798620122f,
                0.4863237397587239f,
                -5.059557506930843f,
                1.339831957561443f,
                -1.6527184543035616f,
                0.08387626826812818f,
                0.47054800692330756f,
                2.8952843774715884f
            ),
            arrayOf(
                -1.4350097067639909f,
                2.5823183098632456f,
                -0.05744843000376091f,
                -0.1042215955598881f,
                0.2396703692733036f,
                -2.655496515562816f,
                0.5392094548932047f,
                -0.3916893177316684f,
                -0.026963380132772902f,
                1.4514149412621213f
            ),
            arrayOf(
                0.6542446726173119f,
                -1.4050530160267736f,
                -0.5503506684993675f,
                -3.1131459640701897f,
                4.476974772228981f,
                -3.2924328422371563f,
                3.8634188724239786f,
                0.7070511712059961f,
                -5.692207292300135f,
                4.262539871575049f
            ),
            arrayOf(
                1.6815240141805916f,
                2.4040746642400426f,
                -1.1211283805500005f,
                -0.3464101016984526f,
                -0.6938256120458677f,
                0.46097849332374385f,
                -0.8883467421386629f,
                0.6001481187511377f,
                -0.8037277568970895f,
                -1.0652792880930892f
            )
        )
    }

}