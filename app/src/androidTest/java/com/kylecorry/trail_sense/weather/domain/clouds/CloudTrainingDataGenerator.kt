package com.kylecorry.trail_sense.weather.domain.clouds

import com.kylecorry.andromeda.core.bitmap.BitmapUtils
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.resizeExact
import com.kylecorry.andromeda.csv.CSVConvert
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.weather.domain.clouds.classification.SoftmaxCloudClassifier
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File

class CloudTrainingDataGenerator {

    @Test
    fun generateTrainingData() {
        // Load images
        val images = CloudGenus.values().flatMap {
            val dir = File("sdcard/Documents/clouds/${it.name.lowercase()}")
            dir.listFiles()?.map { file ->
                it to file
            } ?: emptyList()
        }

        val training = mutableListOf<List<Any>>()
        var i = 0
        for (image in images) {
            val size = SoftmaxCloudClassifier.IMAGE_SIZE
            val original = BitmapUtils.decodeBitmapScaled(image.second.path, size, size)
            val bitmap = original.resizeExact(size, size)
            original.recycle()

            // Calculate training data
            var features = listOf<Float>()
            val classifier = SoftmaxCloudClassifier { features = it }
            runBlocking { classifier.classify(bitmap) }
            // By genus
            val cloudMap = mapOf(
                CloudGenus.Cirrus to 0,
                CloudGenus.Cirrocumulus to 1,
                CloudGenus.Cirrostratus to 2,
                CloudGenus.Altostratus to 3,
                CloudGenus.Altocumulus to 4,
                CloudGenus.Nimbostratus to 5,
                CloudGenus.Stratocumulus to 6,
                CloudGenus.Cumulus to 7,
                CloudGenus.Stratus to 8,
                CloudGenus.Cumulonimbus to 9
            )

            // Add training data sample
            if (features.isNotEmpty()) {
                training.add(listOf(cloudMap[image.first]!!) + features)
            }
            bitmap.recycle()
            i++
            println("Processed $i / ${images.size}")
        }

        // Record training data
        val output = File("sdcard/Documents/clouds.csv")
        output.writeText(CSVConvert.toCSV(training))
    }

}