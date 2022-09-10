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
import com.kylecorry.sol.math.statistics.GLCM
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.shared.ClassificationResult
import com.kylecorry.trail_sense.weather.domain.clouds.mask.ICloudPixelClassifier
import com.kylecorry.trail_sense.weather.domain.clouds.mask.SkyPixelClassification

/**
 * A cloud classifier using the method outlined in: doi:10.5194/amt-3-557-2010
 */
class AMTCloudClassifier(private val pixelClassifier: ICloudPixelClassifier) : ICloudClassifier {

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
        val texture = GLCM.features(glcm)

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

        val blueStdev = Statistics.stdev(cloudBluePixels, mean = blueMean.toFloat())
        val blueSkewness =
            map(
                Statistics.skewness(cloudBluePixels, blueMean.toFloat(), blueStdev),
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
                -6.555916566631804f,
                -3.786142697343315f,
                6.6705213004193284f,
                -1.1557626164186137f,
                -1.2634784103896695f,
                9.29095640470549f,
                -2.0027807362527765f,
                -10.780243025635055f,
                7.6136722731186275f,
                2.4149427446406553f
            ),
            arrayOf(
                -7.27714069668809f,
                -3.377904813086602f,
                -9.602956721844691f,
                -0.7984072379589284f,
                -3.2257646812429104f,
                2.308616562252599f,
                4.682097408570601f,
                11.091088404061136f,
                5.045209796985221f,
                0.9798138884968195f
            ),
            arrayOf(
                9.450900236311426f,
                2.364930327031588f,
                -0.721953936521199f,
                -1.0188069143989058f,
                1.6290420253317792f,
                -1.0708839201864704f,
                -3.3273668429993615f,
                -1.5453419900753123f,
                -1.4932901841502981f,
                -4.184739308022668f
            ),
            arrayOf(
                -0.6056393342297158f,
                0.8994928885473592f,
                -1.339359115009646f,
                -0.7854655165864902f,
                0.1729584122294361f,
                -0.1815923828908561f,
                -0.04785977667131984f,
                1.037762211292898f,
                0.35647037080071303f,
                -0.22715580804885394f
            ),
            arrayOf(
                -6.238168137999249f,
                -0.800559541293349f,
                -4.566331686331202f,
                -0.7008676410084861f,
                -1.208600723076702f,
                1.1643198462912863f,
                3.6875044794774894f,
                5.340989607504892f,
                1.7364954575810227f,
                1.5858275379308928f
            ),
            arrayOf(
                -3.343476887433317f,
                0.03703183206176719f,
                -2.9238706519016326f,
                -0.6781062098963538f,
                0.6211906397288733f,
                0.14976281548356649f,
                2.4648881937708658f,
                3.0545298253925766f,
                0.22054439234066317f,
                0.6522222369034278f
            ),
            arrayOf(
                2.190305595621444f,
                1.7668548387922187f,
                1.5720932389263513f,
                -0.34269544591815304f,
                3.4056305036287995f,
                -0.4311579735019587f,
                -5.780165322513744f,
                -5.081969693663449f,
                4.064601418161343f,
                -1.5874019958436432f
            ),
            arrayOf(
                2.195172634763779f,
                3.3051263707831495f,
                -0.7361610751948466f,
                -0.8597589926071908f,
                5.505690682032209f,
                -5.7581608081086735f,
                0.26681386068650415f,
                0.7553251537246983f,
                -4.473099222089388f,
                -0.38885950647049405f
            ),
            arrayOf(
                -1.2352195791396943f,
                5.919404479134143f,
                2.7116283040416027f,
                -0.283799140848874f,
                2.782933188854201f,
                -4.939721675462307f,
                -1.2707150925594621f,
                5.906650545455611f,
                -10.278452480201986f,
                0.5615308041393454f
            ),
            arrayOf(
                2.0213078172386063f,
                -3.8128739483822707f,
                4.661041358182943f,
                -0.5930885346130648f,
                -10.612761799302174f,
                1.3447748991456685f,
                5.992759613627278f,
                2.1012673341477743f,
                -2.6720725049357026f,
                1.521339363056545f
            ),
            arrayOf(
                -1.0241698145381548f,
                1.439890405415156f,
                1.7864343727092518f,
                -0.055357701406872135f,
                -0.40004809213914644f,
                -5.5144118497307115f,
                2.219278528217467f,
                1.6708195630338838f,
                -1.063016962881692f,
                0.9573103574275336f
            ),
            arrayOf(
                1.0578196201911754f,
                -1.3191947540784321f,
                3.695269145175022f,
                -0.6361607408029998f,
                3.0845244797370412f,
                -5.417158887134944f,
                1.054949848619124f,
                -0.6774051322242334f,
                -3.5419670282747164f,
                2.3316363720916096f
            ),
            arrayOf(
                4.504546757794183f,
                3.80868035790999f,
                0.29339977772896364f,
                -1.3695784754260947f,
                2.998095231373501f,
                -1.6836500502168128f,
                -1.4480915130621854f,
                -1.4757985650992178f,
                -2.8814741946689026f,
                -1.9899986119674007f
            )
        )
    }

}