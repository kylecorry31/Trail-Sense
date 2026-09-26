package com.kylecorry.trail_sense.tools.clouds

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.bitmaps.BitmapUtils.resizeExact
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.trail_sense.tools.clouds.domain.classification.CloudCNNClassifier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudClassifierTest {

    @Test
    fun classifierLoadsWebpWeightsAndReturnsProbabilities(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val assetFiles = AssetFileSystem(instrumentation.context)
        val file = assetFiles.list("clouds/cumulus").first()
        val original: Bitmap = assetFiles.stream("clouds/cumulus/$file").use {
            BitmapFactory.decodeStream(it)
        } ?: error("Unable to decode cloud test image")
        val bitmap = original.resizeExact(CloudCNNClassifier.IMAGE_SIZE, CloudCNNClassifier.IMAGE_SIZE)
        original.recycle()

        val results = CloudCNNClassifier(instrumentation.targetContext).classify(bitmap)
        bitmap.recycle()

        assertEquals(CloudCNNClassifier.CLOUD_GENUSES.size, results.size)
        assertTrue(results.all { it.confidence.isFinite() && it.confidence in 0f..1f })
        assertEquals(1f, results.sumOf { it.confidence.toDouble() }.toFloat(), 0.0001f)
        assertTrue(results.all { it.value != null })
    }

}
