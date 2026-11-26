package com.kylecorry.trail_sense.tools.clouds.domain.classification

import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.bitmaps.BitmapUtils.resizeExact
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoftmaxCloudClassifierTest {

    @Test
    fun identifiesEachCloudGenus() = runBlocking {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val classifier = SoftmaxCloudClassifier()
        val images = assets.list(FOLDER).orEmpty()
        assertEquals(9, images.size)

        val misidentified = images.mapNotNull { file ->
            val expected = expectedGenus(file.substringBeforeLast("."))
            val original = assets.open("$FOLDER/$file").use { BitmapFactory.decodeStream(it) }
            val bitmap = original.resizeExact(SoftmaxCloudClassifier.IMAGE_SIZE, SoftmaxCloudClassifier.IMAGE_SIZE)
            original.recycle()
            val actual = classifier.classify(bitmap).first().value
            bitmap.recycle()
            if (actual == expected) null else "$file was identified as ${actual ?: "clear"}"
        }

        assertTrue(misidentified.joinToString(), misidentified.isEmpty())
    }

    private fun expectedGenus(name: String): CloudGenus? {
        if (name == "clear") {
            return null
        }
        return CloudGenus.entries.first { it.name.lowercase() == name }
    }

    companion object {
        private const val FOLDER = "cloud_tests"
    }
}
