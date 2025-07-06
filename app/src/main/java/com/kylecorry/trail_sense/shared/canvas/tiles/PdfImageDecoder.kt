package com.kylecorry.trail_sense.shared.canvas.tiles

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import com.kylecorry.andromeda.views.subscaleview.decoder.ImageDecoder

class PdfImageDecoder(private val bitmapConfig: Bitmap.Config? = null) : ImageDecoder {

    override fun decode(
        context: Context?,
        uri: Uri
    ): Bitmap {
        val renderer =
            PDFRenderer3(context!!, uri, config = bitmapConfig ?: Bitmap.Config.RGB_565)
        val originalSize = renderer.getSize()
        // Constrict to 1000x1000
        val scale = 1000f / originalSize.width.coerceAtLeast(originalSize.height)
        val newSize =
            Size((originalSize.width * scale).toInt(), (originalSize.height * scale).toInt())
        return renderer.toBitmap(newSize)!!
    }

}