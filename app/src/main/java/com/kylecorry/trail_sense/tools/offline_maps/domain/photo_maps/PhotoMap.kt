package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps

import com.kylecorry.sol.math.MathExtensions.roundNearestAngle
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapState
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapType
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.calibration.MapCalibrationValidationResult
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.calibration.MapCalibrationValidator
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.projections.PhotoMapProjection
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.projections.distancePerPixel
import java.time.Instant

data class PhotoMap(
    override val id: Long,
    override val name: String,
    override val files: List<OfflineMapFile>,
    val georeference: PhotoMapGeoreference,
    override val parentId: Long? = null,
    override val visible: Boolean = true,
    override val createdOn: Instant? = null,
) : OfflineMap {
    override val type = OfflineMapType.Photo
    override val isGroup = false
    override val count: Int? = null

    val imageFile = files.single { it.role == FILE_ROLE_IMAGE }
    val pdfFile = files.singleOrNull { it.role == FILE_ROLE_PDF }

    /**
     * The projection onto the image/pdf.
     */
    val projection: IMapProjection by lazy { PhotoMapProjection(this) }

    override val state by lazy {
        if (MapCalibrationValidator.validate(this) == MapCalibrationValidationResult.Valid) {
            OfflineMapState.Ready
        } else {
            OfflineMapState.Draft
        }
    }

    /**
     * The projection onto the image. Does not use the PDF.
     */
    val imageProjection: IMapProjection by lazy {
        PhotoMapProjection(this, usePdf = false)
    }

    private val _distancePerPixel by lazy {
        if (state != OfflineMapState.Ready) {
            null
        } else {
            projection.distancePerPixel(
                georeference.calibrationPoints[0].location,
                georeference.calibrationPoints[1].location
            )
        }
    }

    /**
     * The distance per pixel of the image
     * @return the distance per pixel or null if the map is not calibrated
     */
    fun distancePerPixel(): Distance? {
        return _distancePerPixel
    }

    /**
     * The rotation of the image to the nearest 90 degrees
     */
    fun baseRotation(): Int {
        return georeference.rotation.roundNearestAngle(90f).toInt()
    }

    /**
     * The size of the image with exact rotation applied
     */
    fun calibratedSize(usePdf: Boolean = true): Size {
        val size = unrotatedSize(usePdf)
        return size.rotate(georeference.rotation)
    }

    /**
     * The size of the image with base rotation applied (ex. 0, 90, 180, 270)
     */
    fun baseSize(usePdf: Boolean = true): Size {
        val size = unrotatedSize(usePdf)
        return size.rotate(baseRotation().toFloat())
    }

    fun unrotatedSize(usePdf: Boolean = true): Size {
        return if (usePdf) {
            georeference.size
        } else {
            georeference.imageSize
        }
    }

    override val bounds: CoordinateBounds? by lazy {
        if (state != OfflineMapState.Ready) {
            null
        } else if (georeference.isFullWorld) {
            CoordinateBounds.world
        } else {
            val size = unrotatedSize()
            val topLeft = projection.toCoordinate(Vector2(0f, 0f))
            val bottomLeft = projection.toCoordinate(Vector2(0f, size.height))
            val topRight = projection.toCoordinate(Vector2(size.width, 0f))
            val bottomRight = projection.toCoordinate(Vector2(size.width, size.height))

            CoordinateBounds.from(listOf(topLeft, bottomLeft, topRight, bottomRight))
        }
    }

    companion object {
        // TODO: Make this based on meters per pixel
        const val DESIRED_PDF_SIZE = 20000
        const val FILE_ROLE_PDF = "pdf"
        const val FILE_ROLE_IMAGE = "image"
    }

}
