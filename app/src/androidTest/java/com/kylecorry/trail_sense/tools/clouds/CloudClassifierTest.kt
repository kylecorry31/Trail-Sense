package com.kylecorry.trail_sense.tools.clouds

import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.bitmaps.BitmapUtils.resizeExact
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.tools.clouds.domain.classification.SoftmaxCloudClassifier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudClassifierTest {

//    @Test
    fun classifyCloudImage() = runBlocking {
        // Load images
        val context = InstrumentationRegistry.getInstrumentation().context
        val assetFiles = AssetFileSystem(context)

        var correct = 0
        var total = 0
        for (genus in CloudGenus.entries) {
            val files = assetFiles.list("clouds/${genus.name.lowercase()}")
            for (file in files) {
                val size = SoftmaxCloudClassifier.IMAGE_SIZE
                val original = assetFiles.stream("clouds/${genus.name.lowercase()}/$file").use {
                    BitmapFactory.decodeStream(it)
                }
                val bitmap = original.resizeExact(size, size)
                original.recycle()

                // Calculate training data
                val classifier = SoftmaxCloudClassifier()
                val classification = classifier.classify(bitmap)

                val actual = classification.maxBy { it.confidence }.value

                if (genus == actual) {
                    correct++
                }

                total++

                bitmap.recycle()
            }
        }

        println("Accuracy: ${correct.toFloat() / total}")
        assertTrue(correct.toFloat() / total > 0.6f)
    }

}