package com.kylecorry.trail_sense.tools.photo_maps.infrastructure

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import com.kylecorry.andromeda.bitmaps.BitmapUtils.rotate
import com.kylecorry.andromeda.files.FileSaver
import com.kylecorry.andromeda.pdf.Datum
import com.kylecorry.andromeda.pdf.GeographicCoordinateSystem
import com.kylecorry.andromeda.pdf.GeospatialPDFParser
import com.kylecorry.andromeda.pdf.PDFObject
import com.kylecorry.andromeda.pdf.PdfConvert
import com.kylecorry.andromeda.pdf.ProjectedCoordinateSystem
import com.kylecorry.andromeda.pdf.Spheroid
import com.kylecorry.andromeda.pdf.bbox
import com.kylecorry.andromeda.pdf.catalog
import com.kylecorry.andromeda.pdf.gcs
import com.kylecorry.andromeda.pdf.geo
import com.kylecorry.andromeda.pdf.image
import com.kylecorry.andromeda.pdf.page
import com.kylecorry.andromeda.pdf.pages
import com.kylecorry.andromeda.pdf.viewport
import com.kylecorry.sol.science.geology.ReferenceEllipsoid
import com.kylecorry.trail_sense.shared.io.ExportService
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.io.UriPicker
import com.kylecorry.trail_sense.shared.io.UriService
import com.kylecorry.trail_sense.tools.photo_maps.domain.MapProjectionType
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap

class MapExportService(
    private val context: Context,
    private val uriPicker: UriPicker,
    private val uriService: UriService
) : ExportService<PhotoMap> {

    private val files = FileSubsystem.getInstance(context)

    override suspend fun export(data: PhotoMap, filename: String): Boolean {
        // If the map was auto calibrated and the PDF exists, just copy it
        if (data.hasPdf(context) && isGeospatialPdf(data.pdfFileName)) {
            val uri = uriPicker.create(filename, "application/pdf") ?: return false
            val outputStream = files.output(uri) ?: return false
            val saver = FileSaver()
            saver.save(files.get(data.pdfFileName), outputStream)
            return true
        }


        val pdf = getPDFData(data)
        val uri = uriPicker.create(filename, "application/pdf") ?: return false
        uriService.outputStream(uri)?.use {
            PdfConvert.toPDF(pdf, it)
        }
        return true
    }

    private suspend fun isGeospatialPdf(filename: String): Boolean {
        val parser = GeospatialPDFParser()
        val metadata = files.stream(files.uri(filename))?.use {
            parser.parse(it)
        }

        return metadata != null
    }

    @Suppress("FoldInitializerAndIfToElvis")
    private fun getPDFData(map: PhotoMap): List<PDFObject> {
        var bitmap: Bitmap? = null
        try {
            val maxImageSize = 2048

            bitmap =
                files.bitmap(map.filename, Size(maxImageSize, maxImageSize)) ?: return emptyList()

            if (map.calibration.rotation != 0f) {
                val rotated = bitmap.rotate(map.calibration.rotation)
                bitmap.recycle()
                bitmap = rotated
            }

            val width = bitmap.width
            val height = bitmap.height
            val bounds = map.boundary()

            if (bounds == null) {
                // No calibration, just generate a PDF containing the image
                return listOf(
                    catalog("1 0", "2 0"),
                    pages("2 0", listOf("3 0")),
                    page("3 0", "2 0", width, height, listOf("4 0")),
                    image("4 0", bitmap, destWidth = width, destHeight = height)
                )
            }

            // Generate a Geospatial PDF
            val projections = mapOf(
                MapProjectionType.Mercator to "Mercator",
                MapProjectionType.CylindricalEquidistant to "Equidistant_Cylindrical"
            )

            val pcjcs = ProjectedCoordinateSystem(
                GeographicCoordinateSystem(
                    Datum(
                        "WGS 84",
                        Spheroid(
                            "WGS 84",
                            ReferenceEllipsoid.wgs84.semiMajorAxis.toFloat(),
                            ReferenceEllipsoid.wgs84.inverseFlattening.toFloat()
                        )
                    ),
                    0.0
                ),
                projections[map.metadata.projection] ?: ""
            )

            return listOf(
                catalog("1 0", "2 0"),
                pages("2 0", listOf("3 0")),
                page("3 0", "2 0", width, height, listOf("4 0"), listOf("5 0")),
                image("4 0", bitmap, destWidth = width, destHeight = height),
                viewport("5 0", "6 0", bbox(0, 0, width, height)),
                geo(
                    "6 0",
                    listOf(bounds.southWest, bounds.northWest, bounds.northEast, bounds.southEast),
                    gcsId = "7 0"
                ),
                gcs("7 0", pcjcs)
            )
        } catch (e: Exception) {
            throw e
        } finally {
            bitmap?.recycle()
        }
    }

}