package com.kylecorry.trail_sense.shared.canvas.tiles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.util.Size
import androidx.core.graphics.toRectF
import com.kylecorry.andromeda.core.math.MathUtils
import com.kylecorry.andromeda.views.subscaleview.decoder.ImageRegionDecoder
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap

class PdfImageRegionDecoder(private val bitmapConfig: Bitmap.Config? = null) : ImageRegionDecoder {

    private lateinit var renderer: PDFRenderer3

    override fun init(
        context: Context?,
        uri: Uri
    ): Point {
        // TODO: Pass in scale
        renderer = PDFRenderer3(context!!, uri, 1f, bitmapConfig ?: Bitmap.Config.RGB_565)
        val size = renderer.getSize()
        val scaledSize = MathUtils.scaleToBounds(
            size,
            Size(PhotoMap.DESIRED_PDF_SIZE, PhotoMap.DESIRED_PDF_SIZE)
        )
        val scale = scaledSize.width.toFloat() / size.width.toFloat()
        renderer = PDFRenderer3(context, uri, scale, bitmapConfig ?: Bitmap.Config.RGB_565)
        return Point(scaledSize.width, scaledSize.height)
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
        renderer.close()
    }

}