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
                -5.180741252149984f,
                -2.11366301240886f,
                -0.9368498847738913f,
                2.217610598485047f,
                5.052980695210264f,
                6.068513999244783f,
                1.658567735452755f,
                -9.334024076766426f,
                0.7217549412547698f,
                1.6569565085992444f
            ),
            arrayOf(
                -0.1435697135532335f,
                -3.4850615022774427f,
                -0.6232928338874214f,
                0.701467915902547f,
                -2.2250595054660307f,
                -1.2621858885814223f,
                1.8368793638045766f,
                5.7779174682674155f,
                0.27512215702427273f,
                -0.610664971903193f
            ),
            arrayOf(
                5.826767550682742f,
                0.4347178462731774f,
                -0.844382266561983f,
                0.3210363393202312f,
                -2.3158322549119106f,
                -0.4777441125697668f,
                -0.8603946326244318f,
                0.9006294926129756f,
                0.5391892566480979f,
                -3.719923370316978f
            ),
            arrayOf(
                -0.2517471469372306f,
                0.5698121978426094f,
                -0.574443397336554f,
                -0.0703298667391579f,
                -0.5617691107949289f,
                0.16809778842437964f,
                -0.5111059702613612f,
                1.3602095671080847f,
                -0.5199264731458574f,
                0.05073024534068396f
            ),
            arrayOf(
                -1.7808969742491403f,
                -0.9594502022596216f,
                -0.5027158719523783f,
                0.15541706705490393f,
                -0.6571049591909777f,
                -0.23517522884801004f,
                0.8596795378967477f,
                3.2728985639732544f,
                -0.51974932822942f,
                0.9951169337303641f
            ),
            arrayOf(
                -0.9629531407868765f,
                -0.11742638003299061f,
                -0.4905994689420209f,
                -0.18636070612299502f,
                -0.6462444816917132f,
                0.06534213818221035f,
                0.831224881537793f,
                2.44523873138625f,
                -0.1287288087292448f,
                0.23768412918286172f
            ),
            arrayOf(
                5.17073114888004f,
                0.35895322676015884f,
                -0.33866416571322316f,
                -0.7844041581589904f,
                -3.4711228736383553f,
                2.5313866401033156f,
                -4.3610132516065f,
                -2.2657114618119696f,
                4.113151179268992f,
                -1.322466927075162f
            ),
            arrayOf(
                -1.2068463931575708f,
                2.790198373912915f,
                -0.8411538392121496f,
                -0.38256850520041813f,
                1.4641515519910993f,
                -2.7688146261077042f,
                1.0898190600509783f,
                1.069448659978021f,
                -1.676887762404615f,
                0.4723790092571382f
            ),
            arrayOf(
                -4.292483613355426f,
                6.987606661206921f,
                -0.3340887284363225f,
                -1.7855350185209327f,
                7.454519918638365f,
                -7.306968203586925f,
                -1.1287419702000652f,
                0.46486905473300993f,
                2.0369390692187945f,
                -1.9242848762246265f
            ),
            arrayOf(
                1.9411062471587694f,
                0.43724275758943243f,
                -0.4389248928367098f,
                0.4493227700755486f,
                -3.6170647154701587f,
                1.3859483024321182f,
                -1.8215360911314278f,
                -1.410376941908588f,
                0.4850733130600228f,
                2.750705394630167f
            ),
            arrayOf(
                -1.3656857092067818f,
                2.738458988585398f,
                -0.21397167480560492f,
                0.013629507016611688f,
                0.16047038558802545f,
                -2.845265797315348f,
                0.22268485002560087f,
                -0.6909588213501047f,
                -0.07004977379758279f,
                1.7656395790764183f
            ),
            arrayOf(
                1.2191463632820052f,
                -0.2961585941080355f,
                -0.2590842979368187f,
                -2.963299434848423f,
                1.4124256254719296f,
                -3.043486873483017f,
                4.598631593352776f,
                0.26477868849754277f,
                -5.614635872031578f,
                4.682374733210872f
            ),
            arrayOf(
                1.6822114636631567f,
                2.081947707933912f,
                -1.2523769697856717f,
                -0.30803829653803794f,
                -1.2032815190132709f,
                0.41503959477044616f,
                -1.2917678272380584f,
                1.5151810012568596f,
                -0.8148816875774841f,
                -1.0574357520405284f
            )
        )
    }

}