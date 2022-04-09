package com.kylecorry.trail_sense.shared.io

import android.content.Context
import com.kylecorry.andromeda.core.bitmap.BitmapUtils
import com.kylecorry.andromeda.files.LocalFiles
import com.kylecorry.andromeda.pdf.*
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.ByteArrayInputStream

class MapExportService(
    private val context: Context,
    private val uriPicker: UriPicker,
    private val uriService: UriService
) :
    ExportService<Map> {
    override suspend fun export(data: Map, filename: String): Boolean {
        try {
            val uri = uriPicker.create(filename, "application/pdf") ?: return false
            val file = LocalFiles.getFile(context, data.filename, create = false)
            
            val size = BitmapUtils.getBitmapSize(file.path)
            val width = if (data.rotation == 90 || data.rotation == 270) {
                size.second
            } else {
                size.first
            }
            val height = if (data.rotation == 90 || data.rotation == 270) {
                size.first
            } else {
                size.second
            }
            val bounds = data.boundary(width.toFloat(), height.toFloat()) ?: return false

            val pdf = listOf(
                catalog("1 0", "2 0"),
                pages("2 0", listOf("3 0")),
                page("3 0", "2 0", width, height, emptyList(), listOf("4 0")),
                // TODO: More efficiently process stream
//                image("4 0", bitmap, destWidth = width, destHeight = height),
                viewport("4 0", "5 0", bbox(0, 0, width, height)),
                geo(
                    "5 0",
                    listOf(bounds.southWest, bounds.northWest, bounds.northEast, bounds.southEast)
                )
            )

            val pdfText = PdfConvert.toPDF(pdf)
            val doc = PDDocument.load(ByteArrayInputStream(pdfText.toByteArray()))
            val page = doc.getPage(0)
            val pdImage = PDImageXObject.createFromFile(file.path, doc)
            PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use {
                it.drawImage(pdImage, 0f, 0f)
            }
            uriService.outputStream(uri)?.use {
                doc.save(it)
            }

            /*
             try (PDPageContentStream contentStream = new PDPageContentStream(doc, page, AppendMode.APPEND, true, true))
61	            {
62	                // contentStream.drawImage(ximage, 20, 20 );
63	                // better method inspired by http://stackoverflow.com/a/22318681/535646
64	                // reduce this value if the image is too large
65	                float scale = 1f;
66	                contentStream.drawImage(pdImage, 20, 20, pdImage.getWidth() * scale, pdImage.getHeight() * scale);
67	            }
68	            doc.save(outputFile);
             */

//            val pdf = listOf(
//                catalog("1 0", "2 0"),
//                pages("2 0", listOf("3 0")),
//                page("3 0", "2 0", width, height, listOf("4 0"), listOf("5 0")),
//                // TODO: More efficiently process stream
//                image("4 0", bitmap, destWidth = width, destHeight = height),
//                viewport("5 0", "6 0", bbox(0, 0, width, height)),
//                geo(
//                    "6 0",
//                    listOf(bounds.southWest, bounds.northWest, bounds.northEast, bounds.southEast)
//                )
//            )
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

}