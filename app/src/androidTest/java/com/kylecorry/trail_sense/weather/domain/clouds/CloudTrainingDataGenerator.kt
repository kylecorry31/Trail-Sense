package com.kylecorry.trail_sense.weather.domain.clouds

import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.core.bitmap.BitmapUtils
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.resize
import com.kylecorry.andromeda.csv.CSVConvert
import com.kylecorry.andromeda.files.LocalFiles
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.weather.domain.clouds.classification.AMTCloudClassifier
import com.kylecorry.trail_sense.weather.domain.clouds.mask.CloudPixelClassifier
import com.kylecorry.trail_sense.weather.domain.clouds.mask.NRBRSkyThresholdCalculator
import kotlinx.coroutines.runBlocking
import org.junit.Test

class CloudTrainingDataGenerator {

    @Test
    fun generateTrainingData() {

        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Load images
        val images = CloudGenus.values().flatMap {
            val dir = LocalFiles.getDirectory(context, "debug/clouds/${it.name.lowercase()}", true)
            dir.listFiles()?.map { file ->
                it to file
            } ?: emptyList()
        }

        val training = mutableListOf<List<Any>>()
        var i = 0
        for (image in images) {
            val size = 500
            val original = BitmapUtils.decodeBitmapScaled(image.second.path, size, size)
            val bitmap = original.resize(size, size)
            original.recycle()

            // Calculate thresholds
            val thresholdCalculator = NRBRSkyThresholdCalculator()
            val skyThreshold = runBlocking { thresholdCalculator.getThreshold(bitmap) }
            val obstacleThreshold = 50

            val pixelClassifier = CloudPixelClassifier.default(
                skyThreshold,
                obstacleThreshold
            )

            // Calculate training data
            var features = listOf<Float>()
            val classifier = AMTCloudClassifier(pixelClassifier) { features = it }
            runBlocking { classifier.classify(bitmap) }

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

            // Add training data sample
            if (features.isNotEmpty()) {
                training.add(listOf(cloudMap.indexOf(image.first)) + features)
            }
            bitmap.recycle()
            i++
            println("Processed $i / ${images.size}")
        }

        // Record training data
        LocalFiles.write(
            context,
            "debug/clouds/clouds.csv",
            CSVConvert.toCSV(training)
        )
    }

}