package com.kylecorry.trail_sense.tools.maps.infrastructure.create

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.core.system.Screen

class PDFRenderer {
    fun toBitmap(
        context: Context,
        uri: Uri,
        page: Int = 0,
        scale: Float = Screen.dpi(context) / 72,
        maxSize: Int = Int.MAX_VALUE,
        @ColorInt backgroundColor: Int = Color.WHITE
    ): Pair<Bitmap, Float>? {
        return context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
            PdfRenderer(fd).use { renderer ->
                val pageCount = renderer.pageCount
                if (page >= pageCount) {
                    return null
                }

                renderer.openPage(page).use { pdfPage ->
                    var actualScale = scale
                    var width = scale * pdfPage.width
                    var height = scale * pdfPage.height

                    if (width > maxSize) {
                        val newScale = maxSize / width
                        width *= newScale
                        height *= newScale
                        actualScale *= newScale
                    }

                    if (height > maxSize) {
                        val newScale = maxSize / height
                        width *= newScale
                        height *= newScale
                        actualScale *= newScale
                    }

                    val bitmap =
                        Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
                    if (backgroundColor != Color.TRANSPARENT) {
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(backgroundColor)
                    }
                    pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap to actualScale
                }
            }
        }
    }
}