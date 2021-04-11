package com.kylecorry.trail_sense.tools.maps.infrastructure

import android.R.attr
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.kylecorry.trailsensecore.infrastructure.view.ViewMeasurementUtils


object PDFUtils {

    fun asBitmap(context: Context, uri: Uri, page: Int = 0): Bitmap? {
        try {
            val fd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            val renderer = PdfRenderer(fd)
            val pageCount = renderer.pageCount
            if (page >= pageCount) {
                return null
            }
            val pdfPage = renderer.openPage(page)
            val width = ViewMeasurementUtils.dpi(context) / 72 * pdfPage.width
            val height = ViewMeasurementUtils.dpi(context) / 72 * pdfPage.height
            val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            pdfPage.close()
            renderer.close()
            return bitmap
        } catch (ex: Exception) {
            return null
        }
    }

}