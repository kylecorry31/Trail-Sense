package com.kylecorry.trail_sense.weather.domain.clouds.classification

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.glcm
import com.kylecorry.andromeda.core.bitmap.ColorChannel
import com.kylecorry.sol.math.SolMath.map
import com.kylecorry.sol.math.classifiers.LogisticRegressionClassifier
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.math.statistics.Texture
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.shared.ClassificationResult
import com.kylecorry.trail_sense.weather.domain.clouds.mask.ICloudPixelClassifier
import com.kylecorry.trail_sense.weather.domain.clouds.mask.SkyPixelClassification

/**
 * A cloud classifier using the method outlined in: doi:10.5194/amt-3-557-2010
 */
class AMTCloudClassifier(
    private val pixelClassifier: ICloudPixelClassifier,
    private val onFeaturesCalculated: (List<Float>) -> Unit = {}
) : ICloudClassifier {

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    override suspend fun classify(bitmap: Bitmap): List<ClassificationResult<CloudGenus>> {
        var skyPixels = 0
        var cloudPixels = 0

        var redMean = 0.0
        var blueMean = 0.0

        val cloudBitmap = bitmap.copy(bitmap.config, true)

        for (w in 0 until bitmap.width) {
            for (h in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(w, h)
                when (pixelClassifier.classify(pixel)) {
                    SkyPixelClassification.Sky -> {
                        skyPixels++
                    }
                    SkyPixelClassification.Obstacle -> {
                        // Do nothing
                    }
                    else -> {
                        cloudPixels++
                        redMean += Color.red(pixel)
                        blueMean += Color.blue(pixel)
                    }
                }
            }
        }

        val levels = 16
        val step = 1
        val windowSize = 50
        val regions = mutableListOf<Rect>()
        for (x in 0 until bitmap.width step windowSize) {
            for (y in 0 until bitmap.height step windowSize) {
                regions.add(Rect(x, y, x + windowSize, y + windowSize))
            }
        }

        val textures = regions.map {
            val glcm = cloudBitmap.glcm(
                listOf(
                    0 to step,
                    step to step,
                    step to 0,
                    step to -step
                ),
                ColorChannel.Red,
                excludeTransparent = true,
                normed = true,
                symmetric = true,
                levels = levels,
                region = it
            )
            Texture.features(glcm)
        }

        cloudBitmap.recycle()

        val cover = if (skyPixels + cloudPixels != 0) {
            cloudPixels / (skyPixels + cloudPixels).toFloat()
        } else {
            0f
        }

        if (cloudPixels != 0) {
            redMean /= cloudPixels
            blueMean /= cloudPixels
        }

        if (cover < 0.05) {
            return emptyList()
        }

        val features = listOf(
            cover,
            // Color
            (redMean / 255).toFloat(),
            percentDifference(redMean, blueMean),
            // Texture
            Statistics.median(textures.map { it.energy }),
            Statistics.median(textures.map { it.correlation }),
            // Bias
            1f
        )

        onFeaturesCalculated(features)

        val classifier = LogisticRegressionClassifier(weights)

        val prediction = classifier.classify(features)

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

        val result = cloudMap.zip(prediction) { genus, confidence ->
            ClassificationResult(
                genus,
                confidence
            )
        }.sortedByDescending { it.confidence }


        return result
    }

    private fun percentDifference(color1: Double, color2: Double): Float {
        return map((color1 - color2).toFloat(), -255f, 255f, 0f, 1f)
    }

    companion object {
        private val weights = arrayOf(
            arrayOf(
                -5.683998421644353f,
                -3.850995741355992f,
                0.02308059779636367f,
                4.310997044454135f,
                -2.916528575634602f,
                4.708308570075787f,
                9.130776723386047f,
                -9.489099073443981f,
                5.496506959220238f,
                -1.4602864951197443f
            ),
            arrayOf(
                -8.23216946781479f,
                -0.22272964241918453f,
                -3.1501686184943267f,
                1.6918591912980034f,
                3.1745966574320894f,
                1.945226507713173f,
                -3.2145356192548316f,
                7.375152546702352f,
                0.11198873724990732f,
                0.3589751704646157f
            ),
            arrayOf(
                -12.697310105879987f,
                2.991832437584434f,
                -1.5689502375051079f,
                0.15047748978355513f,
                -1.6919617754777723f,
                -0.9649174353841071f,
                2.6543096170695217f,
                11.210999904942666f,
                -0.05358461552208588f,
                0.48469025797118925f
            ),
            arrayOf(
                0.9581525341369821f,
                -6.510572656316307f,
                5.498012024331208f,
                5.486731645532976f,
                -7.965314378324742f,
                8.209263592860793f,
                -9.072468763849427f,
                -1.958120601831069f,
                8.038811024477178f,
                -2.122150415945841f
            ),
            arrayOf(
                1.7316310716635575f,
                -2.444244453670931f,
                -3.5525399440972074f,
                -5.5370656561922615f,
                4.2299482430293125f,
                -1.7212642532970108f,
                6.462787318682091f,
                10.968076177729028f,
                -10.654402616200956f,
                0.924722901250349f
            ),
            arrayOf(
                13.861827724816036f,
                8.789448282164278f,
                1.4089087923159063f,
                -3.6536801051425734f,
                3.7970085384700725f,
                -7.932592900069755f,
                -6.03654854760248f,
                -8.675086972206852f,
                -1.5097510902226374f,
                0.02646941842573242f
            )
        )
    }
}