package com.kylecorry.trail_sense.weather.domain.clouds.classification

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
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

        val step = 1
        val glcm = cloudBitmap.glcm(
            listOf(
                step to step
            ),
            ColorChannel.Blue,
            excludeTransparent = true,
            normed = true,
            levels = 128
        )
        cloudBitmap.recycle()
        val texture = Texture.features(glcm)

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

        onFeaturesCalculated(features)

        return result
    }

    private fun percentDifference(color1: Double, color2: Double): Float {
        return map((color1 - color2).toFloat(), -255f, 255f, 0f, 1f)
    }

    companion object {
        private val weights = arrayOf(
            arrayOf(
                -6.372356143677776f,
                -2.9341645640388765f,
                -0.4572288638097959f,
                5.109324393328739f,
                -1.210586393398667f,
                7.0557341230391515f,
                6.501380519588676f,
                -11.725125749108024f,
                5.213256563635377f,
                -1.6722976781256353f
            ),
            arrayOf(
                -13.729478701531988f,
                -2.0875849897302183f,
                -3.774562758267974f,
                1.0236654611362426f,
                3.1483072757389117f,
                -0.38739589807432706f,
                2.9775620314596805f,
                12.120802925922492f,
                -0.5632373220927798f,
                1.1563046699445092f
            ),
            arrayOf(
                9.433997888943857f,
                4.453829658981958f,
                0.34140645359204486f,
                -3.395195282416459f,
                3.5930562538035535f,
                -5.283661757179498f,
                -6.146000221679548f,
                -1.6389262250865257f,
                -2.491003169839899f,
                1.226141702162642f
            ),
            arrayOf(
                1.266537179687531f,
                0.472410221510944f,
                -0.9505420281206322f,
                -1.2506996111061128f,
                -0.12099754933006161f,
                -1.0436593647513561f,
                0.33973265507545347f,
                1.9471148150240263f,
                -1.6662854903053297f,
                0.13807284971030348f
            ),
            arrayOf(
                -7.493603434507942f,
                -1.2761653223964515f,
                -2.104332345077812f,
                0.5700004499297601f,
                0.8880329033716707f,
                0.18655035299587985f,
                4.002570525443681f,
                5.93486064521732f,
                -1.0796048704362917f,
                0.19605485862802857f
            ),
            arrayOf(
                -4.567506806238544f,
                0.2271084253076546f,
                -1.1684696997343373f,
                -0.11545328106695493f,
                2.0756534307854952f,
                -0.8230597399069405f,
                3.001123174696539f,
                2.649029518120099f,
                -1.3628288440600236f,
                0.17048382109848045f
            ),
            arrayOf(
                0.7553274707266608f,
                -4.417518566795713f,
                1.5068852168817173f,
                2.649996288673985f,
                1.942930048698526f,
                2.539304995362276f,
                -0.28935592937686827f,
                -4.3891888822532925f,
                2.3956593246050035f,
                -2.7648107015050076f
            ),
            arrayOf(
                3.4434554147317673f,
                3.899656954929793f,
                -0.5235588573783986f,
                -1.3682822306289313f,
                0.17193568427569483f,
                -4.215919198100575f,
                0.8868043823410405f,
                -0.1470356553395002f,
                -3.342948874556903f,
                0.3627939593567458f
            ),
            arrayOf(
                0.3266016567782459f,
                -1.4819993412661019f,
                -0.0050146968557926936f,
                -0.33808160564871853f,
                2.079165645663637f,
                -1.1242291020874882f,
                0.5541549889156103f,
                1.4282954645516648f,
                -0.3708304600626056f,
                -0.5880373240824841f
            ),
            arrayOf(
                0.6295169157501562f,
                -5.01337373649289f,
                3.4853123642746775f,
                -0.43561671337691754f,
                -10.536811568266645f,
                5.40278085118849f,
                -3.4426641585114544f,
                7.6870111914278825f,
                1.9362854978606676f,
                0.6612343919181838f
            ),
            arrayOf(
                -0.31032246108426f,
                0.029852412214544058f,
                1.4170141082338625f,
                0.8801854296747874f,
                -3.500803629149559f,
                -0.11466354474215601f,
                -0.7781837777292002f,
                1.9295824277372515f,
                -0.2774460201781309f,
                0.7979030901800833f
            ),
            arrayOf(
                1.9697329399707666f,
                -0.1410134255026984f,
                0.7654479528570032f,
                -0.24491449215893515f,
                0.6332846153426647f,
                -2.2629287438593195f,
                -2.12980356614044f,
                0.9204458993257354f,
                3.0736163325466994f,
                -2.2509752877373654f
            ),
            arrayOf(
                8.076166039417265f,
                4.264658234918866f,
                0.04524750938820229f,
                -3.489481335680129f,
                2.225866324599957f,
                -4.372411836064155f,
                -1.0941848200609265f,
                -2.1392865016496043f,
                -3.893528087147594f,
                0.5613909942311791f
            )
        )
    }
}