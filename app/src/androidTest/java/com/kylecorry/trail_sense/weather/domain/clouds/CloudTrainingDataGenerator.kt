package com.kylecorry.trail_sense.weather.domain.clouds

import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.core.bitmap.BitmapUtils
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.resizeExact
import com.kylecorry.andromeda.csv.CSVConvert
import com.kylecorry.andromeda.files.LocalFiles
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.weather.domain.clouds.classification.TextureCloudClassifier
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
            val size = TextureCloudClassifier.IMAGE_SIZE
            val original = BitmapUtils.decodeBitmapScaled(image.second.path, size, size)
            val bitmap = original.resizeExact(size, size)
            original.recycle()

            // Calculate training data
            var features = listOf<Float>()
            val classifier = TextureCloudClassifier { features = it }
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

//            // By altitude
//            val cloudMap = mapOf(
//                CloudGenus.Cirrus to 0,
//                CloudGenus.Cirrocumulus to 0,
//                CloudGenus.Cirrostratus to 0,
//                CloudGenus.Altostratus to 1,
//                CloudGenus.Altocumulus to 1,
//                CloudGenus.Nimbostratus to 2,
//                CloudGenus.Stratocumulus to 2,
//                CloudGenus.Cumulus to 2,
//                CloudGenus.Stratus to 2,
//                CloudGenus.Cumulonimbus to 2
//            )

//            // By form
//            val cloudMap = mapOf(
//                CloudGenus.Cirrus to 0,
//                CloudGenus.Cirrocumulus to 1,
//                CloudGenus.Cirrostratus to 2,
//                CloudGenus.Altostratus to 2,
//                CloudGenus.Altocumulus to 1,
//                CloudGenus.Nimbostratus to 2,
//                CloudGenus.Stratocumulus to 2,
//                CloudGenus.Cumulus to 1,
//                CloudGenus.Stratus to 2,
//                CloudGenus.Cumulonimbus to 1
//            )

            // Add training data sample
            if (features.isNotEmpty()) {
                training.add(listOf(cloudMap[image.first]!!) + features)
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