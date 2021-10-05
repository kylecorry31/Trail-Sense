package com.kylecorry.trail_sense.weather.infrastructure.clouds

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.sol.math.classifiers.LogisticRegressionClassifier
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.weather.domain.clouds.*
import com.kylecorry.trail_sense.weather.domain.clouds.GLCMUtils.glcm

class CloudAnalyzer(
    private val skyDetectionSensitivity: Int,
    private val obstacleRemovalSensitivity: Int,
    private val skyColorOverlay: Int,
    private val excludedColorOverlay: Int,
    private val cloudColorOverlay: Int,
) {

    private val cloudService = CloudService()

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    suspend fun getClouds(
        bitmap: Bitmap,
        setPixel: (x: Int, y: Int, pixel: Int) -> Unit = { _, _, _ -> }
    ): CloudObservation {
        var bluePixels = 0
        var cloudPixels = 0
        var luminance = 0.0

        val isSky = BGIsSkySpecification(100 - skyDetectionSensitivity)

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
                        val lum = average(pixel)
                        luminance += lum
                        setPixel(w, h, cloudColorOverlay)
                    }
                }
            }
        }

        val glcm: GLCM = cloudBitmap.glcm(1 to 1, ColorChannel.Blue, true)
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

        val clouds = mutableListOf<CloudGenus>()

        val classifier = LogisticRegressionClassifier(
            arrayOf(
                arrayOf(
                    -0.23776002100912258f,
                    -0.07536706154580863f,
                    -0.3697220019933562f,
                    0.8282447214591838f,
                    0.16642897959887396f,
                    0.4889988117680018f,
                    0.12328572374513762f,
                    -0.24970016025076572f,
                    -0.28878708481120313f,
                    -0.2511043173953318f
                ),
                arrayOf(
                    -0.08540014752981644f,
                    0.4142393531375055f,
                    -0.0974056443726276f,
                    -0.2734206530970552f,
                    0.2218010692690537f,
                    -0.36128417785886013f,
                    -0.14060454507055764f,
                    0.3792446004378641f,
                    -0.1935153535374707f,
                    -0.08310024332567287f
                ),
                arrayOf(
                    -0.011757235848464914f,
                    -0.16947817799348736f,
                    0.01822814705924382f,
                    0.5548301050724468f,
                    -0.10155242172968874f,
                    0.13565794024664857f,
                    0.044480637990979836f,
                    -0.07258350957808446f,
                    -0.09249222492166209f,
                    -0.13690455988849498f
                ),
                arrayOf(
                    -0.34841510719172053f,
                    0.2528167864908148f,
                    -0.2623734297742721f,
                    0.06864979427348472f,
                    0.13039471627415974f,
                    0.19164314568615523f,
                    -0.1969000444916424f,
                    0.49797406197959143f,
                    -0.28651013431131034f,
                    -0.2847000021852047f
                ),
                arrayOf(
                    -0.11183967535743995f,
                    0.12495068826328107f,
                    -0.3229448830530763f,
                    0.2946486979162687f,
                    -0.009457097617503431f,
                    0.1222942548433649f,
                    -0.18103497733134213f,
                    0.08368601723690115f,
                    -0.2526457676518806f,
                    -0.3663631526304381f
                ),
                arrayOf(
                    -0.3065275237824141f,
                    0.045957829454357516f,
                    -0.20583301331056997f,
                    0.2944763758058261f,
                    0.07509360562299439f,
                    0.17272938247555145f,
                    -0.12339456943417605f,
                    0.6241517562778807f,
                    -0.23986971029546023f,
                    -0.38972351876536715f
                ),
                arrayOf(
                    -0.5309136121599751f,
                    0.23461297512683688f,
                    -0.4614931356957752f,
                    0.4553183837175689f,
                    0.28832929940097435f,
                    0.28631492013885806f,
                    -0.1937322080796167f,
                    0.7602802540426262f,
                    -0.334696171984927f,
                    -0.3172735660309286f
                )
            )
        )

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
            luminance.toFloat(),
            features.contrast / 255f,
            features.energy * 100,
            features.entropy  / 16f,
            features.homogeneity,
            result
        )
    }


    private fun average(@ColorInt color: Int): Float {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        return (r + g + b).toFloat() / 3f
    }

}