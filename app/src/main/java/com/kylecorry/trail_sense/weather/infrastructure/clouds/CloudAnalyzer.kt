package com.kylecorry.trail_sense.weather.infrastructure.clouds

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.sol.math.SolMath.map
import com.kylecorry.sol.math.classifiers.LogisticRegressionClassifier
import com.kylecorry.sol.math.statistics.GLCMService
import com.kylecorry.sol.math.statistics.StatisticsService
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.weather.domain.clouds.BrightnessIsObstacleSpecification
import com.kylecorry.trail_sense.weather.domain.clouds.ColorChannel
import com.kylecorry.trail_sense.weather.domain.clouds.GLCMUtils.glcm
import com.kylecorry.trail_sense.weather.domain.clouds.NRBRIsSkySpecification
import com.kylecorry.trail_sense.weather.domain.clouds.SaturationIsObstacleSpecification

class CloudAnalyzer(
    private val skyDetectionSensitivity: Int,
    private val obstacleRemovalSensitivity: Int,
    private val skyColorOverlay: Int,
    private val excludedColorOverlay: Int,
    private val cloudColorOverlay: Int,
) {

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    suspend fun getClouds(
        bitmap: Bitmap,
        setPixel: (x: Int, y: Int, pixel: Int) -> Unit = { _, _, _ -> }
    ): CloudObservation {
        var bluePixels = 0
        var cloudPixels = 0
        var luminance = 0.0

        var redMean = 0.0
        var greenMean = 0.0
        var blueMean = 0.0

        val blue = mutableListOf<Float>()

        val isSky = NRBRIsSkySpecification(skyDetectionSensitivity / 200f)

        val isObstacle =
            SaturationIsObstacleSpecification(1 - obstacleRemovalSensitivity / 100f).or(
                BrightnessIsObstacleSpecification(obstacleRemovalSensitivity.toFloat())
            )

        val cloudBitmap = bitmap.copy(bitmap.config, true)

        for (w in 0 until bitmap.width) {
            for (h in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(w, h)

                when {
                    isSky.isSatisfiedBy(pixel) -> {
                        bluePixels++
                        setPixel(w, h, skyColorOverlay)
                        cloudBitmap.setPixel(w, h, Color.TRANSPARENT)
                    }
                    isObstacle.isSatisfiedBy(pixel) -> {
                        setPixel(w, h, excludedColorOverlay)
                        cloudBitmap.setPixel(w, h, Color.TRANSPARENT)
                    }
                    else -> {
                        cloudPixels++
                        redMean += Color.red(pixel)
                        blueMean += Color.blue(pixel)
                        greenMean += Color.green(pixel)
                        blue.add(Color.blue(pixel).toFloat())
                        val lum = average(pixel)
                        luminance += lum
                        setPixel(w, h, cloudColorOverlay)
                    }
                }
            }
        }

        val glcm = cloudBitmap.glcm(1 to 1, ColorChannel.Blue, true)
        cloudBitmap.recycle()
        val features = GLCMService().features(glcm)


        val cover = if (bluePixels + cloudPixels != 0) {
            cloudPixels / (bluePixels + cloudPixels).toFloat()
        } else {
            0f
        }

        luminance = if (cloudPixels != 0) {
            luminance / cloudPixels
        } else {
            0.0
        }

        if (cloudPixels != 0) {
            redMean /= cloudPixels
            greenMean /= cloudPixels
            blueMean /= cloudPixels
        }

        val statistics = StatisticsService()

        val blueStdev = statistics.stdev(blue, mean = blueMean.toFloat())

        blue.clear()

        // TODO: Get blue standard deviation
        // TODO: Get blue skewness

        val classifier = LogisticRegressionClassifier(weights)

        val prediction = classifier.classify(
            listOf(
                cover,
                features.contrast / 255f,
                features.energy * 100,
                features.entropy / 16f,
                features.homogeneity,
                luminance.toFloat(),
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
            cloudMap[index] to confidence
        }.sortedByDescending { it.second }

        return CloudObservation(
            cover,
            features.contrast / 255f,
            features.energy * 100,
            features.entropy / 16f,
            features.homogeneity,
            (redMean / 255).toFloat(),
            (blueMean / 255).toFloat(),
            percentDifference(redMean, greenMean),
            percentDifference(redMean, blueMean),
            percentDifference(greenMean, blueMean),
            blueStdev / 255f,
            result
        )
    }

    private fun percentDifference(color1: Double, color2: Double): Float {
        return map((color1 - color2).toFloat(), -255f, 255f, 0f, 1f)
    }


    private fun average(@ColorInt color: Int): Float {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        return (r + g + b).toFloat() / 3f
    }

    companion object {
        private val weights = arrayOf(
            arrayOf(
                -0.2898198973690522f,
                -0.1972710626226795f,
                -0.3946345540531277f,
                -0.046535050002773366f,
                0.31393556081037916f,
                0.7763551745043425f,
                0.13836110769526944f,
                -0.04580979457187086f,
                -0.14598809579330205f,
                -0.3198422933295196f
            ),
            arrayOf(
                -0.06858739672841892f,
                0.014591695369760849f,
                -0.03759178564835933f,
                -0.22315122174184052f,
                0.7911843908976979f,
                -0.2687109760911666f,
                -0.08638820355003896f,
                -0.036676952138120215f,
                -0.16191267475847704f,
                -0.079819543444428f
            ),
            arrayOf(
                -0.015535605248998725f,
                -0.043638347146587216f,
                -0.1329519058141921f,
                0.0036646218451094064f,
                -0.032037051298058186f,
                0.3022437127981791f,
                -0.01580949679724595f,
                -0.04039467761744187f,
                0.029265236500311067f,
                0.12476164916779758f
            ),
            arrayOf(
                -0.21103473058113978f,
                -0.02490197391610435f,
                -0.3205470885376532f,
                -0.0898010384418755f,
                0.5635543688688606f,
                0.04978773559648901f,
                -0.02211533349011549f,
                0.4340060284440327f,
                -0.39361278655354415f,
                -0.24940267641773398f
            ),
            arrayOf(
                -0.12007592119641682f,
                -0.2277043581569911f,
                -0.029697437966563243f,
                -0.025691585866493026f,
                0.033124607603647695f,
                0.4497834092954544f,
                -0.06231975939148763f,
                0.015360815981438522f,
                -0.07837060693409216f,
                -0.08401965994460706f
            ),
            arrayOf(
                -0.452358675753025f,
                0.1839369469078505f,
                -0.3622934683896456f,
                -0.04342992173863559f,
                0.09204821558923373f,
                0.2713445988897073f,
                -0.11355923235988942f,
                0.5047440080213553f,
                -0.08348615773226313f,
                -0.19271381579830899f
            ),
            arrayOf(
                -0.49911495269524175f,
                0.11734227516270658f,
                -0.42319689541965455f,
                -0.057589328543881296f,
                0.11609191226488731f,
                0.35580943989588987f,
                0.06554611826613302f,
                0.6650417784549751f,
                -0.21488884308512451f,
                -0.17539566175778684f
            )
        )
    }

}