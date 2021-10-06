package com.kylecorry.trail_sense.weather.infrastructure.clouds

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.annotation.ColorInt
import com.kylecorry.sol.math.SolMath.map
import com.kylecorry.sol.math.SolMath.power
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.math.classifiers.LogisticRegressionClassifier
import com.kylecorry.sol.math.statistics.GLCMService
import com.kylecorry.sol.math.statistics.StatisticsService
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.shared.specifications.FalseSpecification
import com.kylecorry.trail_sense.weather.domain.clouds.*
import com.kylecorry.trail_sense.weather.domain.clouds.GLCMUtils.glcm

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
        var luminance = 0.0

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
                        val lum = average(pixel)
                        luminance += lum
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
                -0.10683397339043026f,
                0.11332452458975625f,
                -0.12000267498790095f,
                0.1799604872122538f,
                0.4597066089514793f,
                0.6272653436156262f,
                -0.055318560098278916f,
                -0.28156028623290036f,
                -0.19434674878391145f,
                -0.26612354808255445f
            ),
            arrayOf(
                -0.11703870198709104f,
                0.33494269496189066f,
                -0.20158322379074686f,
                0.009125976833703399f,
                0.03517544611757878f,
                0.39973010614280285f,
                0.06972971342240114f,
                -0.051847216696695377f,
                -0.05961017849495767f,
                -0.06269821400629451f
            ),
            arrayOf(
                -0.16166024717563152f,
                0.2569168277302536f,
                -0.21851350273508513f,
                0.036617572783692126f,
                0.03166692789868289f,
                0.692417879937132f,
                -0.12200417302680575f,
                -0.2222683092399644f,
                -0.3247417326446898f,
                -0.2898155150615714f
            ),
            arrayOf(
                -0.2566310970562635f,
                0.2062443823777467f,
                -0.25401528243809124f,
                0.12909853141182998f,
                0.26522522547319854f,
                0.18042369711395706f,
                -0.12772002692997952f,
                -0.05511763173897439f,
                -0.18499023106991927f,
                -0.027365657952872963f
            ),
            arrayOf(
                -0.1148228131093223f,
                0.013625117259733265f,
                -0.07999296526835635f,
                0.2144841549601411f,
                0.25652372878954716f,
                0.2503500861104831f,
                -0.014901265770154971f,
                -0.07038713776169075f,
                -0.13699472474320273f,
                -0.03631385442951625f
            ),
            arrayOf(
                -0.1651679148832698f,
                0.16022682757791143f,
                -0.09681758998439997f,
                0.014637884074801204f,
                0.2953111330627528f,
                0.32242784455222856f,
                -0.047195589636882175f,
                -0.15433464768949426f,
                0.03366236825985981f,
                -0.1944244797820573f
            ),
            arrayOf(
                -0.05759439786792277f,
                -0.21453707245427517f,
                -0.03723522245048359f,
                0.0398993107453148f,
                0.09006200681125628f,
                0.4535016965504328f,
                -0.16506203305524106f,
                -0.06105808467259529f,
                -0.15388431177823175f,
                0.005403879088535663f
            ),
            arrayOf(
                -0.12305206440567153f,
                0.4645985437281187f,
                -0.25682974882142207f,
                0.09830064346941635f,
                0.14748725562085685f,
                0.03965180041082947f,
                0.04142910148631795f,
                -0.27795671626424623f,
                -0.1901326606654899f,
                -0.25612383869259225f
            ),
            arrayOf(
                -0.13492905992214446f,
                0.6623929864177415f,
                -0.1643299036826781f,
                -0.06286119918724867f,
                0.652329068087235f,
                -0.4947265168437538f,
                -0.1486112350381617f,
                0.0015316184578616184f,
                -0.09973428326663067f,
                -0.11328178505189866f
            ),
            arrayOf(
                -0.08639528221004536f,
                0.09829227155604067f,
                -0.22811976118084218f,
                0.16769037589172486f,
                -0.08024581466163122f,
                0.49262123852134787f,
                0.14906232502840572f,
                -0.1662481205105325f,
                -0.2512720634341354f,
                -0.2585669561735884f
            ),
            arrayOf(
                -0.06851287814989594f,
                0.005553047959075055f,
                -0.07168442977760899f,
                -0.18495906549394733f,
                0.08108049472191198f,
                0.01562915413091568f,
                0.14595204627785605f,
                -0.06085935414349177f,
                0.01469511242797922f,
                -0.05238418788477795f
            ),
            arrayOf(
                -0.2814947472342375f,
                0.39889295849242457f,
                -0.07466853600050286f,
                -0.10273510097536792f,
                0.2140187903388635f,
                0.11682545900720731f,
                0.14061563728781348f,
                -0.07677783591686621f,
                -0.08697565899079751f,
                -0.11386705625612357f
            ),
            arrayOf(
                -0.4428007622872329f,
                0.5378188001086796f,
                -0.41140763177238454f,
                0.21407366149096946f,
                0.252287627562475f,
                0.4958674929729158f,
                -0.04932747112155004f,
                -0.3846905883701173f,
                -0.35677470418046975f,
                -0.2296352647137692f
            )
        )
    }

}