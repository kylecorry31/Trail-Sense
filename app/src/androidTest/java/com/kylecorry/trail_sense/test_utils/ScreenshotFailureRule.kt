package com.kylecorry.trail_sense.test_utils

import android.graphics.Bitmap.CompressFormat
import android.util.Base64
import android.util.Log
import androidx.test.core.app.takeScreenshot
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.ByteArrayOutputStream


class ScreenshotFailureRule : TestWatcher() {

    override fun failed(e: Throwable?, description: Description?) {
        super.failed(e, description)
        val screenshot = takeScreenshot()
        val stream = ByteArrayOutputStream()
        screenshot.compress(CompressFormat.WEBP, 25, stream)
        val bitmapBytes = stream.toByteArray()
        val encodedImage = Base64.encodeToString(bitmapBytes, Base64.DEFAULT)
        val chunked = encodedImage.chunked(1000)
        chunked.forEach {
            Log.e("Screenshot", it)
        }
    }

}