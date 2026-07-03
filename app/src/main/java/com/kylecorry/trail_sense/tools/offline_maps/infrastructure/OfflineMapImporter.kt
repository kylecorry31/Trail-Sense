package com.kylecorry.trail_sense.tools.offline_maps.infrastructure

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.core.units.PercentCoordinate
import com.kylecorry.andromeda.pdf.GeospatialPDFParser
import com.kylecorry.andromeda.pdf.PDFRenderer
import com.kylecorry.andromeda.pdf.PDFRenderer2
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.offline_maps.domain.CreateOfflineMapRequest
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMapGeoreference
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.calibration.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.projections.MapProjectionType
import com.kylecorry.trail_sense.tools.offline_maps.domain.trail_maps.TrailMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.trail_maps.mapsforge.MapsforgeAdapter
import java.io.IOException
import java.time.Instant
import java.util.UUID

internal class OfflineMapImporter(
    private val context: Context,
    private val files: FileSubsystem,
    private val prefs: UserPreferences
) {

    suspend fun import(request: CreateOfflineMapRequest): OfflineMap? = onIO {
        val type = files.getMimeType(request.uri) ?: getMimeTypeFromExtension(request.uri)
        when {
            type == MIME_TYPE_PDF -> importPdf(request)
            type?.startsWith(MIME_TYPE_IMAGE_PREFIX) == true -> importImage(request)
            else -> importTrailMap(request)
        }
    }

    private suspend fun importImage(request: CreateOfflineMapRequest): PhotoMap? {
        val file = files.copyToLocal(request.uri, PHOTO_MAPS_DIRECTORY) ?: return null
        var rotation = 0
        tryOrLog {
            val exif = ExifInterface(file)
            rotation = exif.rotationDegrees
        }

        val path = files.getLocalPath(file)
        val imageSize = files.imageSize(path)
        val fileSize = files.size(path)

        return PhotoMap(
            0,
            request.name,
            listOf(
                OfflineMapFile(path, fileSize, PhotoMap.FILE_ROLE_IMAGE)
            ),
            PhotoMapGeoreference(
                Size(imageSize.width.toFloat(), imageSize.height.toFloat()),
                rotation = rotation.toFloat(),
                isWarpingCompleted = request.photoMapCalibration != null,
                calibrationPoints = request.photoMapCalibration.orEmpty()
            ),
            parentId = request.parentId,
            visible = request.visible,
            createdOn = Instant.now()
        )
    }

    private suspend fun importPdf(request: CreateOfflineMapRequest): PhotoMap? {
        val uuid = UUID.randomUUID().toString()
        val filename = "$PHOTO_MAPS_DIRECTORY/$uuid.webp"
        val pdfFilename = "$PHOTO_MAPS_DIRECTORY/$uuid.pdf"
        val calibrationPoints = mutableListOf<MapCalibrationPoint>()
        var projection = MapProjectionType.CylindricalEquidistant

        val parser = GeospatialPDFParser()
        val metadata = files.stream(request.uri)?.use {
            parser.parse(it)
        }
        val maxSize = 2048
        val (bp, scale) = PDFRenderer().toBitmap(context, request.uri, maxSize = maxSize)
            ?: return null

        if (metadata != null && metadata.points.size >= 4) {
            val first = metadata.points[0]
            val second = metadata.points.maxBy { it.first.distanceTo(first.first) }

            val points = listOf(first, second).map {
                MapCalibrationPoint(
                    Coordinate(it.second.latitude, it.second.longitude),
                    PercentCoordinate(scale * it.first.x / bp.width, scale * it.first.y / bp.height)
                )
            }
            calibrationPoints.addAll(points)
        }

        val projectionName = metadata?.projection?.projection
        if (projectionName == null || projectionName.contains("mercator", true)) {
            projection = MapProjectionType.Mercator
        }

        try {
            files.save(filename, bp, recycleOnSave = true)
        } catch (e: IOException) {
            Log.e("OfflineMapImporter", "Failed to save image", e)
            return null
        }

        tryOrNothing {
            files.copyToLocal(request.uri, PHOTO_MAPS_DIRECTORY, "$uuid.pdf")
        }

        val imageSize = files.imageSize(filename)
        val fileSize = files.size(filename)

        val pdfSize = tryOrDefault(null) {
            PDFRenderer2(context, request.uri).use {
                it.getSize()
            }
        }

        val requestedCalibration = request.photoMapCalibration
        return PhotoMap(
            0,
            request.name,
            listOf(
                OfflineMapFile(filename, fileSize, PhotoMap.FILE_ROLE_IMAGE),
                OfflineMapFile(pdfFilename, files.size(pdfFilename), PhotoMap.FILE_ROLE_PDF)
            ),
            PhotoMapGeoreference(
                Size(imageSize.width.toFloat(), imageSize.height.toFloat()),
                pdfSize?.let { Size(it.width.toFloat(), it.height.toFloat()) },
                projectionType = projection,
                isWarpingCompleted = requestedCalibration != null || calibrationPoints.isNotEmpty(),
                calibrationPoints = requestedCalibration ?: calibrationPoints
            ),
            parentId = request.parentId,
            visible = request.visible,
            createdOn = Instant.now()
        )
    }

    private suspend fun importTrailMap(request: CreateOfflineMapRequest): TrailMap? {
        if (!MapsforgeAdapter.isMapsforgeMap(request.uri)) {
            return null
        }
        val path = if (prefs.photoMaps.copyTrailMapsToAppStorage) {
            copyToAppStorage(request.uri, MapsforgeAdapter.MAPSFORGE_MAP_EXTENSION) ?: return null
        } else {
            val canPersist = tryOrDefault(false) {
                files.acceptPersistentAccess(request.uri)
                true
            }
            if (canPersist) {
                request.uri.toString()
            } else {
                throw IllegalStateException("Can't persist access to ${request.uri}")
            }
        }

        val info = MapsforgeAdapter.getMapInfo(path) ?: return null

        return TrailMap(
            0,
            request.name,
            listOf(
                OfflineMapFile(path, files.size(path), TrailMap.FILE_ROLE_MAPSFORGE_MAP)
            ),
            Instant.now(),
            info.bounds,
            info.attribution,
            visible = request.visible,
            parentId = request.parentId
        )
    }

    private suspend fun copyToAppStorage(uri: Uri, extension: String): String? {
        val saved =
            files.copyToLocal(uri, OFFLINE_MAPS_DIRECTORY, "${UUID.randomUUID()}.$extension")
                ?: return null
        return files.getLocalPath(saved)
    }

    private fun getMimeTypeFromExtension(uri: Uri): String? {
        val filename = files.getFileName(uri, withExtension = true, fallbackToPathName = true)
            ?: uri.lastPathSegment
            ?: return null

        return when (filename.substringAfterLast('.', "").lowercase()) {
            "pdf" -> MIME_TYPE_PDF
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> null
        }
    }

    private companion object {
        private const val MIME_TYPE_PDF = "application/pdf"
        private const val MIME_TYPE_IMAGE_PREFIX = "image/"
        private const val PHOTO_MAPS_DIRECTORY = "maps"
        private const val OFFLINE_MAPS_DIRECTORY = "offline_maps"
    }
}
