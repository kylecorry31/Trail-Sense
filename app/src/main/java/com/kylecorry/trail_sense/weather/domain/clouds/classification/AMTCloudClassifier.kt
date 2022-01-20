package com.kylecorry.trail_sense.weather.domain.clouds.classification

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
import com.kylecorry.trail_sense.shared.ClassificationResult
import com.kylecorry.trail_sense.weather.domain.clouds.mask.ICloudPixelClassifier
import com.kylecorry.trail_sense.weather.domain.clouds.mask.SkyPixelClassification

/**
 * A cloud classifier using the method outlined in: doi:10.5194/amt-3-557-2010
 */
class AMTCloudClassifier(private val pixelClassifier: ICloudPixelClassifier) : ICloudClassifier {

    private val statistics = StatisticsService()

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    override suspend fun classify(bitmap: Bitmap): List<ClassificationResult<CloudGenus>> {
        var skyPixels = 0
        var cloudPixels = 0

        var redMean = 0.0
        var greenMean = 0.0
        var blueMean = 0.0

        val cloudBluePixels = mutableListOf<Float>()

        val cloudBitmap = bitmap.copy(bitmap.config, true)

        for (w in 0 until bitmap.width) {
            for (h in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(w, h)

                when (pixelClassifier.classify(pixel)) {
                    SkyPixelClassification.Sky -> {
                        skyPixels++
                        cloudBitmap.setPixel(w, h, Color.TRANSPARENT)
                    }
                    SkyPixelClassification.Obstacle -> {
                        cloudBitmap.setPixel(w, h, Color.TRANSPARENT)
                    }
                    else -> {
                        cloudPixels++
                        redMean += Color.red(pixel)
                        blueMean += Color.blue(pixel)
                        greenMean += Color.green(pixel)
                        cloudBluePixels.add(Color.blue(pixel).toFloat())
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

        val features = listOf(
            cover,
            (redMean / 255).toFloat(),
            (blueMean / 255).toFloat(),
            percentDifference(redMean, greenMean),
            percentDifference(redMean, blueMean),
            percentDifference(greenMean, blueMean),
            texture.energy * 100,
            texture.entropy / 16f,
            texture.contrast / 255f,
            texture.homogeneity,
            blueStdev / 255f,
            blueSkewness,
            1f
        )

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

        logFeatures(features)

        return result
    }

    /**
     * Logs an observation to the console in CSV training format
     */
    private fun logFeatures(features: List<Float>) {
        Log.d("CloudFeatures", features.joinToString(",") {
            if (it.isNaN()) {
                it
            } else {
                it.roundPlaces(2)
            }.toString()
        })
    }

    private fun percentDifference(color1: Double, color2: Double): Float {
        return map((color1 - color2).toFloat(), -255f, 255f, 0f, 1f)
    }

    companion object {
        private val weights = arrayOf(
            arrayOf(
                -5.18552922416379f,
                -3.06552919635344f,
                5.630919282330972f,
                -1.0402257526827192f,
                -0.501282593216022f,
                8.279151203222147f,
                -1.743812940979361f,
                -10.21445021535074f,
                6.1535010121901985f,
                1.9858546100814238f
            ),
            arrayOf(
                -5.819970089466201f,
                -3.967847810421361f,
                -8.193540332030636f,
                -0.8693204372862111f,
                -1.9626137998918984f,
                1.4006651102398056f,
                3.9595324071849753f,
                10.223798715178813f,
                3.9432055306887768f,
                1.0250237686309587f
            ),
            arrayOf(
                7.9040587948809184f,
                2.3237073231692302f,
                -1.6517112424348146f,
                -1.0114870948091927f,
                0.07869286439987351f,
                -1.0503776763748063f,
                -2.6700442766085932f,
                -0.5270389075967432f,
                0.5830641633659778f,
                -3.8999716363881345f
            ),
            arrayOf(
                -0.19785189476096285f,
                0.5134722556112852f,
                -0.7389015297481394f,
                -0.6010689098834389f,
                0.9936559510424562f,
                -0.37072216660818796f,
                0.31261209809630996f,
                0.9780469597488632f,
                -0.521805417550936f,
                -0.37387090668571193f
            ),
            arrayOf(
                -5.11063952596972f,
                -1.3612650263033224f,
                -3.123572552112743f,
                -0.659043785964492f,
                -0.09696257375626091f,
                0.7447357216925974f,
                3.0818994375402515f,
                4.711885659354107f,
                0.0934928706716745f,
                1.5712576111452567f
            ),
            arrayOf(
                -3.250266244981929f,
                -0.11837098038292174f,
                -2.192244010626855f,
                -0.4838294824731894f,
                1.0352424387729071f,
                0.10321323469823855f,
                2.0035453561066876f,
                2.652343838500159f,
                -0.29153544279865706f,
                0.6266029413045626f
            ),
            arrayOf(
                4.954885552037183f,
                2.094834761721526f,
                -0.7019080848257274f,
                -0.23490496591423438f,
                -5.032029429059721f,
                1.596012649166592f,
                -4.735104191508511f,
                -3.6537955648738794f,
                7.317583482737285f,
                -1.2866880997176204f
            ),
            arrayOf(
                0.7034679631787115f,
                2.908193513621995f,
                -0.08074189385149193f,
                -0.9302031738929506f,
                6.483290411771163f,
                -4.928635182219099f,
                0.9816280089857907f,
                -0.034811269677751375f,
                -5.135006909119145f,
                -0.0578518241650548f
            ),
            arrayOf(
                -0.4830827985779288f,
                5.9434578472383075f,
                2.048139513574755f,
                -0.3282047449388521f,
                1.624200645685947f,
                -5.900948537902213f,
                -1.7634090387184294f,
                5.846677638896944f,
                -7.827936882848698f,
                0.3695166193758423f
            ),
            arrayOf(
                3.0318954452053566f,
                -3.3494418432184503f,
                4.484181261861153f,
                -0.60013411954767f,
                -10.746563812167027f,
                2.4358606130644103f,
                3.558879089473426f,
                1.1620348702319538f,
                -1.5121831458100277f,
                1.6557807823369601f
            ),
            arrayOf(
                -1.1649279666157932f,
                1.2444180352949084f,
                2.0300983192009108f,
                -0.11876362800206643f,
                0.1716682455523934f,
                -4.518875156812747f,
                2.4975490803860696f,
                1.1162276783735925f,
                -2.2662547781921663f,
                1.2488642535115089f
            ),
            arrayOf(
                0.6994031185282225f,
                -0.5526972297644506f,
                3.079115539941791f,
                -0.7298540801105914f,
                1.0730501715862621f,
                -4.960391173666323f,
                1.2759346545772032f,
                -0.8570983642517174f,
                -1.4415303626314921f,
                2.3573080538595494f
            ),
            arrayOf(
                3.3320237649507174f,
                3.64790205232964f,
                0.41415442658845936f,
                -1.3278721757847796f,
                2.689546927629937f,
                -1.340180941784199f,
                -1.011761717731488f,
                -0.9997552633251137f,
                -2.956901203705678f,
                -1.9090512110203175f
            )
        )
    }

}