package com.kylecorry.trail_sense.weather.domain.clouds

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.glcm
import com.kylecorry.andromeda.core.bitmap.ColorChannel
import com.kylecorry.sol.math.SolMath.map
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
                -6.167164472057479f,
                -1.6360487782359916f,
                4.997650953468637f,
                -1.127738925925236f,
                -0.5202542202994013f,
                7.019920904321908f,
                0.4257368608817683f,
                -9.457488319531608f,
                4.917660232143823f,
                1.8265521798304307f
            ),
            arrayOf(
                -3.3945945070379815f,
                -4.671008135971499f,
                -8.632329124737028f,
                -0.9271611364646528f,
                -1.3974545661959905f,
                1.2141337335735756f,
                3.978989700974361f,
                9.043487658205759f,
                3.9124121999878634f,
                0.5679908563146133f
            ),
            arrayOf(
                7.651685887965588f,
                1.3331324470064492f,
                -2.602448104674599f,
                -0.905515721338657f,
                0.07784935386591066f,
                -0.18765674584738457f,
                -0.6920856641859472f,
                -1.880133397479474f,
                0.9868543427202171f,
                -3.810371222683126f
            ),
            arrayOf(
                -0.07931064177156683f,
                0.41119566844900324f,
                -0.6713440650423045f,
                -0.6979094315504593f,
                1.0298272219594475f,
                0.03661835136895539f,
                -0.47445753020277415f,
                1.2102596903550589f,
                -0.2224544925085622f,
                -0.3429467481172363f
            ),
            arrayOf(
                -4.081725378874473f,
                -1.7328867203305804f,
                -2.9340292466065017f,
                -0.6353072191506471f,
                0.3959432103374537f,
                0.6263177440908257f,
                1.705940277931278f,
                5.0026663619690925f,
                0.5538580593209456f,
                1.3248577342113073f
            ),
            arrayOf(
                -2.6287154016706853f,
                -0.52226165637964f,
                -2.063779111642381f,
                -0.6181400093072074f,
                0.856561338813847f,
                0.3768778543768461f,
                1.2174275530836742f,
                3.031475715938542f,
                -0.11608586265834804f,
                0.5065903046255784f
            ),
            arrayOf(
                4.707206274046424f,
                0.8194227834177611f,
                0.16581703100263995f,
                -0.15691640749090047f,
                -3.126807835868146f,
                -0.10214713090422128f,
                -4.633280187228092f,
                -2.3046499402198872f,
                5.635079242588774f,
                -0.9688319547583618f
            ),
            arrayOf(
                -0.21678157698731945f,
                4.063256439318621f,
                -0.1971717445797866f,
                -0.7517253419451813f,
                3.743192880478186f,
                -2.747812303066733f,
                1.5572204827624663f,
                -0.33426812826754415f,
                -4.738947148753226f,
                -0.3897308991138945f
            ),
            arrayOf(
                -1.8089794113228825f,
                6.47911468310299f,
                0.7691500642796245f,
                -0.12524015281797524f,
                2.94518325018959f,
                -5.780611698238736f,
                -1.1177964079421823f,
                6.846799906021123f,
                -7.570178273458256f,
                -0.3288708623210252f
            ),
            arrayOf(
                2.1651228410967165f,
                -2.140897963275573f,
                6.171756970701295f,
                -0.5297348986259858f,
                -6.860393624240266f,
                0.7883394923969909f,
                -1.1671918418500218f,
                0.8791464305531248f,
                -2.49634566265018f,
                2.5228253597479937f
            ),
            arrayOf(
                -1.6557567249125102f,
                2.408286860614239f,
                2.6072504344953864f,
                -0.07253472029226125f,
                -1.1002673013422655f,
                -3.3663729792998636f,
                1.0974607718873752f,
                1.1514246444804734f,
                -2.510866069915833f,
                1.3723770223963319f
            ),
            arrayOf(
                2.3663979305696254f,
                -2.820588061285371f,
                4.638369987417706f,
                -0.6914986466343133f,
                1.191514758348508f,
                -7.8400973562711505f,
                1.4889935865556003f,
                -0.9584706232098472f,
                -0.8639700998409637f,
                3.3298021522324177f
            ),
            arrayOf(
                2.740743356303852f,
                3.3107569951940437f,
                0.4806061196389897f,
                -1.3053836257889153f,
                2.025276673330468f,
                -0.5000439175651543f,
                -1.7000716354466283f,
                -0.8375718540469864f,
                -2.3238807218796227f,
                -1.7524684612781085f
            )
        )
    }

}