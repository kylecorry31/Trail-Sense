package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.sol.math.MathExtensions.roundNearestAngle
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.projections.PhotoMapProjection
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.projections.distancePerPixel
import java.time.Instant

data class PhotoMap(
    override val id: Long,
    override val name: String,
    val filename: String,
    val fileSizeBytes: Long,
    val georeference: PhotoMapGeoreference,
    override val parentId: Long? = null,
    val visible: Boolean = true,
    val isAsset: Boolean = false,
    val createdOn: Instant? = null,
) : IMap {
    override val isGroup = false
    override val count: Int? = null

    val pdfFileName = filename.replace(".webp", "") + ".pdf"

    private val hooks = Hooks()

    /**
     * The projection onto the image/pdf.
     */
    val projection: IMapProjection by lazy { PhotoMapProjection(this) }

    /**
     * The projection onto the image. Does not use the PDF.
     */
    val imageProjection: IMapProjection by lazy {
        PhotoMapProjection(this, usePdf = false)
    }

    /**
     * Determines if the map is calibrated
     */
    val isCalibrated: Boolean
        get() = MapCalibrationValidator.validate(this) == MapCalibrationValidationResult.Valid

    /**
     * The distance per pixel of the image
     * @return the distance per pixel or null if the map is not calibrated
     */
    fun distancePerPixel(): Distance? {

        if (!isCalibrated) {
            return null
        }

        return hooks.memo("distance_per_pixel") {
            projection.distancePerPixel(
                georeference.calibrationPoints[0].location,
                georeference.calibrationPoints[1].location
            )
        }
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

    /**
     * The boundary of the image
     * @return the boundary or null if the map is not calibrated
     */
    fun boundary(): CoordinateBounds? {
        return hooks.memo("boundary") {
            PhotoMapBoundsCalculator().calculate(this)
        }
    }

    fun hasPdf(context: Context): Boolean {
        return AppServiceRegistry.get<FileSubsystem>().get(pdfFileName).exists()
    }

    companion object {
        // TODO: Make this based on meters per pixel
        const val DESIRED_PDF_SIZE = 20000
    }

}
