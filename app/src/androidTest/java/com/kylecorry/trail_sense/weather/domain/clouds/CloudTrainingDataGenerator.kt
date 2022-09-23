package com.kylecorry.trail_sense.weather.domain.clouds

import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.core.bitmap.BitmapUtils
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.resizeExact
import com.kylecorry.andromeda.csv.CSVConvert
import com.kylecorry.andromeda.files.FileSaver
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.weather.domain.clouds.classification.SoftmaxCloudClassifier
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File

class CloudTrainingDataGenerator {

    @Test
    fun generateTrainingData() {
        /*
            Before running this test ensure the androidTest/assets/clouds is populated with folders for each cloud genus.

            Use the lowercase name of each CloudGenus enum as the folder name and place all images of that cloud in the folder.

            Not supported yet, but use "clear" as the folder name for images without clouds.
         */


        // Load images
        val context = InstrumentationRegistry.getInstrumentation().context
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val saver = FileSaver()
        File(appContext.cacheDir.path + "/clouds").deleteRecursively()
        val images = CloudGenus.values().flatMap {
            val files = context.assets.list("clouds/${it.name.lowercase()}")
            File(appContext.cacheDir.path + "/clouds/${it.name.lowercase()}").mkdirs()
            files?.map { file ->
                val f = File(appContext.cacheDir.path + "/clouds/${it.name.lowercase()}/$file")
                f.createNewFile()
                val stream = context.assets.open("clouds/${it.name.lowercase()}/$file")
                saver.save(stream, f)
                it to f
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

        File(appContext.cacheDir.path + "/clouds").deleteRecursively()
    }

}