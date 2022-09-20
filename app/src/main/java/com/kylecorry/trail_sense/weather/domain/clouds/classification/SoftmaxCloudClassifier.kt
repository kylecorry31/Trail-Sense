package com.kylecorry.trail_sense.weather.domain.clouds.classification

import android.graphics.Bitmap
import android.graphics.Rect
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.glcm
import com.kylecorry.andromeda.core.bitmap.ColorChannel
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.classifiers.LogisticRegressionClassifier
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.math.statistics.Texture
import com.kylecorry.sol.math.statistics.TextureFeatures
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.shared.ClassificationResult
import com.kylecorry.trail_sense.shared.colors.ColorUtils
import kotlin.math.sqrt

class SoftmaxCloudClassifier(
    private val onFeaturesCalculated: (List<Float>) -> Unit = {}
) : ICloudClassifier {

    override suspend fun classify(bitmap: Bitmap): List<ClassificationResult<CloudGenus?>> {
        var averageNRBR = 0.0

        for (w in 0 until bitmap.width) {
            for (h in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(w, h)
                averageNRBR += ColorUtils.nrbr(pixel)
            }
        }

        averageNRBR /= bitmap.width * bitmap.height

        val regions = mutableListOf<Rect>()
        for (x in 0 until bitmap.width step GLCM_WINDOW_SIZE) {
            for (y in 0 until bitmap.height step GLCM_WINDOW_SIZE) {
                regions.add(Rect(x, y, x + GLCM_WINDOW_SIZE, y + GLCM_WINDOW_SIZE))
            }
        }

        val textures = regions.map {
            val glcm = bitmap.glcm(
                listOf(
                    0 to GLCM_STEP_SIZE,
                    GLCM_STEP_SIZE to GLCM_STEP_SIZE,
                    GLCM_STEP_SIZE to 0,
                    GLCM_STEP_SIZE to -GLCM_STEP_SIZE
                ),
                ColorChannel.Red,
                normed = true,
                symmetric = true,
                levels = GLCM_LEVELS,
                region = it
            )
            Texture.features(glcm)
        }

        val texture = TextureFeatures(
            textures.map { it.energy }.avg(),
            textures.map { it.entropy }.avg(),
            textures.map { it.contrast }.avg(),
            textures.map { it.homogeneity }.avg(),
            textures.map { it.dissimilarity }.avg(),
            textures.map { it.angularSecondMoment }.avg(),
            textures.map { it.horizontalMean }.avg(),
            textures.map { it.verticalMean }.avg(),
            textures.map { it.horizontalVariance }.avg(),
            textures.map { it.verticalVariance }.avg(),
            textures.map { it.correlation }.avg(),
            textures.map { it.max }.avg(),
        )

        val features = listOf(
            // Color
            SolMath.norm(averageNRBR.toFloat(), -1f, 1f) * 2,
            // Texture
            texture.energy,
            texture.contrast,
            SolMath.norm(texture.verticalMean, 0f, GLCM_LEVELS.toFloat()),
            SolMath.norm(sqrt(texture.verticalVariance), 0f, 3f),
            // Bias
            1f
        )

        onFeaturesCalculated(features)

        val isClear = features[0] < 0.55 && features[4] < 0.15f

        val classifier = LogisticRegressionClassifier.fromWeights(weights)

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
            ClassificationResult<CloudGenus?>(
                genus,
                confidence * (if (isClear) 0.5f else 1f)
            )
        } + listOf(
            ClassificationResult<CloudGenus?>(
                null,
                if (isClear) 0.5f else 0f
            )
        )


        return result.sortedByDescending { it.confidence }
    }

    private fun List<Float>.avg(): Float {
        return Statistics.mean(this)
    }

    companion object {
        const val IMAGE_SIZE = 400
        private const val GLCM_LEVELS = 16
        private const val GLCM_WINDOW_SIZE = IMAGE_SIZE / 4
        private const val GLCM_STEP_SIZE = 1
        private val weights = arrayOf(
            arrayOf(
                -9.455291886552716f,
                -5.529273509622504f,
                1.2065397677633543f,
                1.2942317663005285f,
                2.293825769651199f,
                3.4536953462376943f,
                9.647234665124858f,
                -4.1242535793211905f,
                2.407888277175005f,
                -1.1182752250719166f
            ),
            arrayOf(
                0.18636950807953298f,
                -8.747776353655098f,
                5.452264998827839f,
                -0.35911764102922566f,
                -8.603026793230137f,
                4.285516495093585f,
                -2.0801003229730286f,
                8.320033490393724f,
                1.162004287682592f,
                -0.04646037846803479f
            ),
            arrayOf(
                -0.9298648057182556f,
                4.150394757581087f,
                -0.9082485093962868f,
                -2.077398930942411f,
                4.2750949685609845f,
                1.855222837295111f,
                -2.2642203151651232f,
                -1.1247565812874052f,
                -2.3652623587103836f,
                -0.9085187194435138f
            ),
            arrayOf(
                -6.925748683924586f,
                -1.1697693273870886f,
                -3.171879237369924f,
                -0.3568579733310823f,
                2.714280513515399f,
                8.803094313992927f,
                2.718158928911068f,
                -4.044381323561874f,
                1.8148882772384f,
                -0.2700125866226722f
            ),
            arrayOf(
                -2.3162599713370815f,
                -3.592191989487161f,
                -2.784331416635153f,
                -1.7840051336890426f,
                0.1245760479195146f,
                -6.259950465596728f,
                6.29331286200891f,
                13.129016966802732f,
                -4.43089879815486f,
                2.020735050585396f
            ),
            arrayOf(
                13.132217488221098f,
                11.234823825951004f,
                -1.0250668431941798f,
                -1.2151538349699724f,
                1.8465555977537638f,
                -7.744806979361771f,
                -8.757451158899999f,
                -4.072433268200014f,
                -2.417565535982144f,
                -0.9149695804696036f
            )
        )
    }
}