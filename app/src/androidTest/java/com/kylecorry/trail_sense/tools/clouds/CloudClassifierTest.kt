package com.kylecorry.trail_sense.tools.clouds

import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.bitmaps.BitmapUtils.resizeExact
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.test_utils.ManualOnlyTest
import com.kylecorry.trail_sense.tools.clouds.domain.classification.SoftmaxCloudClassifier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudClassifierTest {

    @ManualOnlyTest
    @Test
    fun classifyCloudImage() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().context
        val assetFiles = AssetFileSystem(context)
        val classifier = SoftmaxCloudClassifier()

        var correct = 0
        var total = 0
        val folders = CloudGenus.entries.map { it to it.name.lowercase() } + (null to "clear")
        for ((genus, folder) in folders) {
            for (file in assetFiles.list("clouds/$folder")) {
                val size = SoftmaxCloudClassifier.IMAGE_SIZE
                val original = assetFiles.stream("clouds/$folder/$file").use {
                    BitmapFactory.decodeStream(it)
                }
                val bitmap = original.resizeExact(size, size)
                original.recycle()

                val actual = classifier.classify(bitmap).maxBy { it.confidence }.value
                println("CloudClassifierTest: $folder/$file -> ${actual?.name ?: "clear"}")

                if (genus == actual) {
                    correct++
                }
                total++
                bitmap.recycle()
            }
        }

        println("CloudClassifierTest: accuracy ${correct.toFloat() / total} over $total images")
        assertTrue(correct.toFloat() / total > 0.6f)
    }

}
