package com.kylecorry.trail_sense.shared.canvas.tiles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Size
import androidx.annotation.ColorInt
import androidx.core.graphics.createBitmap
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.trail_sense.shared.bitmaps.Convert
import com.kylecorry.trail_sense.shared.bitmaps.applyOperations

class PDFRenderer3(
    private val context: Context,
    private val uri: Uri,
    private val inchesToPixels: Float? = null,
    private val config: Bitmap.Config = Bitmap.Config.RGB_565,
) {

    private val dpi = Screen.dpi(context)
    private val lock = Any()

    private var fd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private var page: Int = 0
    private var pdfPage: PdfRenderer.Page? = null

    private fun open(page: Int): PdfRenderer.Page {
        synchronized(lock) {
            if (fd == null) {
                fd = context.contentResolver.openFileDescriptor(uri, "r")
            }
            if (renderer == null) {
                renderer = PdfRenderer(fd!!)
            }

            if (this.page != page || pdfPage == null) {
                this.page = page
                pdfPage?.close()
                pdfPage = renderer!!.openPage(page)
            }

            return pdfPage!!
        }
    }

    fun close() {
        synchronized(lock) {
            pdfPage?.close()
            renderer?.close()
            fd?.close()
            fd = null
            renderer = null
            pdfPage = null
        }
    }

    fun getSize(): Size {
        val pdfPage = open(0)
        return Size(pdfPage.width, pdfPage.height)
    }

    fun toBitmap(
        outputSize: Size,
        page: Int = 0,
        @ColorInt backgroundColor: Int = Color.WHITE,
        srcRegion: RectF? = null
    ): Bitmap? {
        val pdfPage = open(page)

        val bitmap =
            createBitmap(outputSize.width, outputSize.height)
        if (backgroundColor != Color.TRANSPARENT) {
            bitmap.eraseColor(backgroundColor)
        }

        val transform = if (srcRegion != null) {
            val matrix = Matrix()

            val dpiScale = inchesToPixels ?: (dpi / 72f)

            // Scale the PDF to the screen DPI
            matrix.postScale(dpiScale, dpiScale)

            // Translate the PDF to the top left corner
            matrix.postTranslate(-srcRegion.left, -srcRegion.top)

            // Scale the PDF to the output size
            matrix.postScale(
                outputSize.width.toFloat() / srcRegion.width(),
                outputSize.height.toFloat() / srcRegion.height()
            )

            matrix
        } else {
            null
        }

        pdfPage.render(
            bitmap,
            null,
            transform,
            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
        )

        return bitmap.applyOperations(Convert(config))
    }
}