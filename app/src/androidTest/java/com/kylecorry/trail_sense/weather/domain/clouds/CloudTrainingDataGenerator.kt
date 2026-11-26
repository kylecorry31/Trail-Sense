package com.kylecorry.trail_sense.tools.weather.domain.clouds

import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.bitmaps.BitmapUtils
import com.kylecorry.andromeda.bitmaps.BitmapUtils.resizeExact
import com.kylecorry.andromeda.csv.CSVConvert
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.andromeda.files.BaseFileSystem
import com.kylecorry.andromeda.files.CacheFileSystem
import com.kylecorry.andromeda.files.FileSaver
import com.kylecorry.trail_sense.test_utils.ManualOnlyTest
import com.kylecorry.trail_sense.tools.clouds.domain.classification.SoftmaxCloudClassifier
import kotlinx.coroutines.runBlocking
import org.junit.Test

class CloudTrainingDataGenerator {

    @ManualOnlyTest
    @Test
    fun generateTrainingData() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val saver = FileSaver()
        val cacheFiles = CacheFileSystem(appContext)
        val assetFiles = AssetFileSystem(context)
        val documentFiles = BaseFileSystem(appContext, "sdcard/Documents")

        val training = mutableListOf<List<Any>>()
        CLASS_FOLDERS.forEachIndexed { label, folder ->
            for (file in assetFiles.list("clouds/$folder")) {
                val cached = cacheFiles.getFile("clouds/$folder/$file", true)
                saver.save(runBlocking { assetFiles.stream("clouds/$folder/$file") }, cached)

                val size = SoftmaxCloudClassifier.IMAGE_SIZE
                val original = BitmapUtils.decodeBitmapScaled(cached.path, size, size) ?: continue
                val bitmap = original.resizeExact(size, size)
                original.recycle()

                var features = listOf<Float>()
                runBlocking { SoftmaxCloudClassifier { features = it }.classify(bitmap) }
                training.add(listOf(label, "$folder/$file") + features)
                bitmap.recycle()
                println("Processed ${training.size}")
            }
        }

        documentFiles.write("clouds.csv", CSVConvert.toCSV(training))
        cacheFiles.delete("clouds", true)
    }

    companion object {
        private val CLASS_FOLDERS = listOf(
            "cirrus",
            "cirrocumulus",
            "cirrostratus",
            "altostratus",
            "altocumulus",
            "nimbostratus",
            "stratocumulus",
            "cumulus",
            "stratus",
            "cumulonimbus",
            "clear"
        )
    }
}
