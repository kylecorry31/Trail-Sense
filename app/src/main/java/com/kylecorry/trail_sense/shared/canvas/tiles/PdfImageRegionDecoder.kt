package com.kylecorry.trail_sense.shared.canvas.tiles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.util.Size
import androidx.core.graphics.toRectF
import com.kylecorry.andromeda.pdf.PDFRenderer2
import com.kylecorry.andromeda.views.subscaleview.decoder.ImageRegionDecoder
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap

class PdfImageRegionDecoder(private val bitmapConfig: Bitmap.Config? = null) : ImageRegionDecoder {

    private lateinit var renderer: PDFRenderer2

    override fun init(
        context: Context?,
        uri: Uri
    ): Point {
        val scale = PhotoMap.PDF_SCALE
        renderer = PDFRenderer2(context!!, uri, scale, bitmapConfig ?: Bitmap.Config.RGB_565)
        val size = renderer.getSize()
        return Point((size.width * scale).toInt(), (size.height * scale).toInt())
    }

    override fun decodeRegion(
        sRect: Rect,
        sampleSize: Int
    ): Bitmap {
        val scaledSize = Size(sRect.width() / sampleSize, sRect.height() / sampleSize)
        return renderer.toBitmap(
            scaledSize,
            srcRegion = sRect.toRectF()
        )!!
    }

    override fun isReady(): Boolean {
        return this::renderer.isInitialized
    }

    override fun recycle() {
        // Do nothing
    }

}