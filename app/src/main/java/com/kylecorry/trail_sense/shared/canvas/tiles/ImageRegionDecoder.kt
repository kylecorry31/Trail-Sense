package com.kylecorry.trail_sense.shared.canvas.tiles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.trail_sense.shared.andromeda_temp.CoroutineObjectPool
import com.kylecorry.trail_sense.shared.andromeda_temp.use

class ImageRegionDecoder(
    private val context: Context,
    private val bitmapConfig: Bitmap.Config = Bitmap.Config.RGB_565,
    maxDecoders: Int = maxOf(4, Runtime.getRuntime().availableProcessors())
) : RegionDecoder {

    private val pool = CoroutineObjectPool(maxDecoders, cleanup = { it.recycle() }) {
        createDecoder()
    }
    private var uri: Uri? = null
    private var assetPath: String? = null

    fun init(uri: Uri) {
        this.uri = uri
        assetPath = null
    }

    fun initFromAsset(assetPath: String) {
        uri = null
        this.assetPath = assetPath
    }

    private fun createDecoder(): BitmapRegionDecoder {
        val inputStream = when {
            uri != null -> context.contentResolver.openInputStream(uri!!)
            assetPath != null -> context.assets.open(assetPath!!)
            else -> null
        } ?: error("Unable to open file")

        return inputStream.use { stream ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                BitmapRegionDecoder.newInstance(stream)
            } else {
                @Suppress("DEPRECATION")
                BitmapRegionDecoder.newInstance(stream, false)
            }
        } ?: error("Failed to create BitmapRegionDecoder")
    }

    override suspend fun decodeRegionSuspend(sRect: Rect, sampleSize: Int): Bitmap? = onIO {
        pool.use { decoder ->
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = bitmapConfig
            }
            decoder.decodeRegion(sRect, options)
        }
    }

    override suspend fun recycleSuspend() {
        pool.close()
    }
}
