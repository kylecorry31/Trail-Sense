package com.kylecorry.trail_sense.tools.maps.infrastructure

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.geo.cartography.MapCalibrationPoint
import com.kylecorry.trailsensecore.domain.math.toDoubleCompat
import com.kylecorry.trailsensecore.domain.math.toFloatCompat
import com.kylecorry.trailsensecore.domain.pixels.PercentCoordinate
import com.kylecorry.andromeda.files.ExternalFileService
import com.kylecorry.trailsensecore.infrastructure.view.ViewMeasurementUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


object PDFUtils {

    fun asBitmap(context: Context, uri: Uri, page: Int = 0): Bitmap? {

        try {
            val fd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            val renderer = PdfRenderer(fd)
            val pageCount = renderer.pageCount
            if (page >= pageCount) {
                renderer.close()
                fd.close()
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
            fd.close()
            return bitmap
        } catch (ex: Exception) {
            return null
        }
    }

    suspend fun getGeospatialCalibration(context: Context, uri: Uri): List<MapCalibrationPoint> {
        return withContext(Dispatchers.IO) {
            // TODO: Only load the heading
            val text = ExternalFileService(context).read(uri) ?: return@withContext listOf<MapCalibrationPoint>()
            // /Type\s*/Measure\s*/Subtype\s*/GEO\s|.*/GPTS\s*\[(.*)]
            // /Type\s*/Viewport\s|.*/BBox\s*\[(.*)]
            val geoMatches = Regex("/GPTS\\s*\\[(.*)]").find(text)
            val viewportMatches = Regex("/BBox\\s*\\[(.*)]").find(text)
            val mediaBox = Regex("/MediaBox\\s*\\[(.*)]").find(text)
            // Geo type /Bounds tell which point matches what [left|right top|bottom] left = 0, right = 1, top = 0, bottom = 1
            if (geoMatches != null && viewportMatches != null && mediaBox != null) {
                val geo = geoMatches.groupValues[1].split(" ").mapNotNull { it.toDoubleCompat() }
                val box = mediaBox.groupValues[1].split(" ").mapNotNull { it.toFloatCompat() }
                val viewport =
                    viewportMatches.groupValues[1].split(" ").mapNotNull { it.toFloatCompat() }
                if (geo.size == 8 && box.size == 4 && viewport.size == 4) {
                    val width = box[2]
                    val height = box[3]
                    val topLeftPct =
                        PercentCoordinate(viewport[0] / width, 1 - viewport[1] / height)
                    val bottomRightPct =
                        PercentCoordinate(viewport[2] / width, 1 - viewport[3] / height)
                    val topLeft = Coordinate(geo[2], geo[3])
                    val bottomRight = Coordinate(geo[6], geo[7])
                    return@withContext listOf(
                        MapCalibrationPoint(topLeft, topLeftPct),
                        MapCalibrationPoint(bottomRight, bottomRightPct)
                    )
                }
            }
            listOf()
        }
    }
}