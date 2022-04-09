package com.kylecorry.trail_sense.tools.maps.infrastructure.create

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.kylecorry.andromeda.files.ExternalFiles
import com.kylecorry.andromeda.files.LocalFiles
import com.kylecorry.andromeda.pdf.GeospatialPDFParser
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.domain.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.maps.domain.MapProjectionType
import com.kylecorry.trail_sense.tools.maps.domain.PercentCoordinate
import com.kylecorry.trail_sense.tools.maps.infrastructure.IMapRepo
import com.kylecorry.trail_sense.tools.maps.infrastructure.ImageSaver
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class CreateMapFromPDFCommand(private val context: Context, private val repo: IMapRepo) {
    suspend fun execute(uri: Uri): Map? = onIO {
        val defaultName = context.getString(android.R.string.untitled)
        val filename = "maps/" + UUID.randomUUID().toString() + ".webp"
        val calibrationPoints = mutableListOf<MapCalibrationPoint>()
        var projection = MapProjectionType.CylindricalEquidistant

        val parser = GeospatialPDFParser()
        val metadata = ExternalFiles.stream(context, uri)?.use {
            parser.parse(it)
        }
        val maxSize = 2048
        val (bp, scale) = PDFRenderer().toBitmap(context, uri, maxSize = maxSize) ?: return@onIO null

        if (metadata != null && metadata.points.size >= 4) {
            val points = listOf(metadata.points[1], metadata.points[3]).map {
                MapCalibrationPoint(
                    it.second,
                    PercentCoordinate(scale * it.first.x / bp.width, scale * it.first.y / bp.height)
                )
            }
            calibrationPoints.addAll(points)
        }

        val projectionName = metadata?.projection?.projection
        if (projectionName != null && projectionName.contains("mercator", true)) {
            projection = MapProjectionType.Mercator
        }

        try {
            copyToLocalStorage(bp, filename)
        } catch (e: IOException) {
            return@onIO null
        }

        val map = Map(
            0,
            defaultName,
            filename,
            calibrationPoints,
            warped = true,
            rotated = true,
            projection = projection
        )

        val id = repo.addMap(map)
        map.copy(id = id)
    }

    private fun copyToLocalStorage(bitmap: Bitmap, filename: String) {
        try {
            @Suppress("BlockingMethodInNonBlockingContext")
            FileOutputStream(LocalFiles.getFile(context, filename)).use { out ->
                // TODO: Figure out why WEBP doesn't work here for the newly generated maps
                ImageSaver().save(bitmap, out)
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 60, out)
            }
        } finally {
            bitmap.recycle()
        }
    }
}