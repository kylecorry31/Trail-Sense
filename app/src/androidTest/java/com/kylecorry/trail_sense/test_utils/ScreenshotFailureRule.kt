package com.kylecorry.trail_sense.test_utils

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.util.Base64
import android.util.Log
import androidx.test.core.app.takeScreenshot
import androidx.test.platform.io.PlatformTestStorageRegistry
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.ByteArrayOutputStream

/**
 * Saves a screenshot when a test fails. The test storage service keeps its output outside of the
 * app's data, so it survives the Orchestrator clearing the package between tests, and AGP copies
 * it to app/build/outputs/connected_android_test_additional_output after the run.
 */
class ScreenshotFailureRule : TestWatcher() {

    override fun failed(e: Throwable?, description: Description) {
        super.failed(e, description)
        val screenshot = takeScreenshot()
        try {
            saveScreenshot(screenshot, description)
        } catch (t: Throwable) {
            Log.e("Screenshot", "Unable to save screenshot, dumping to logcat", t)
            logScreenshot(screenshot)
        }
    }

    private fun saveScreenshot(screenshot: Bitmap, description: Description) {
        val name = sanitize("${description.testClass?.simpleName}-${description.methodName}")
        PlatformTestStorageRegistry.getInstance().openOutputFile("$name.png").use {
            screenshot.compress(CompressFormat.PNG, 100, it)
        }
    }

    private fun sanitize(name: String): String {
        return name.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }

    private fun logScreenshot(screenshot: Bitmap) {
        val stream = ByteArrayOutputStream()
        screenshot.compress(CompressFormat.WEBP, 25, stream)
        val encodedImage = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
        encodedImage.chunked(1000).forEach {
            Log.e("Screenshot", it)
        }
    }

}
