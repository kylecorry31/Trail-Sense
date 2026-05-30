package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.tiles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import com.kylecorry.andromeda.bitmaps.BitmapUtils
import com.kylecorry.andromeda.bitmaps.operations.BitmapOperation
import com.kylecorry.andromeda.bitmaps.operations.Conditional
import com.kylecorry.andromeda.bitmaps.operations.CorrectPerspective
import com.kylecorry.andromeda.bitmaps.operations.Resize
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.math.arithmetic.Arithmetic
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.trail_sense.shared.extensions.toAndroidSize
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.PhotoMapWarp
import kotlin.math.roundToInt

class PhotoMapRegionLoader(
    private val context: Context,
    val map: PhotoMap,
    private val decoderCache: PhotoMapDecoderCache,
    private val loadPdfs: Boolean = true,
    private val isPixelPerfect: Boolean = false,
    private val operations: List<BitmapOperation> = emptyList(),
    private val sampleSizeMultiplier: Float = 1f
) {

    suspend fun load(tile: Tile): Bitmap? = onIO {
        val bounds = tile.getBounds()
        val maxSize = tile.size
        val projection = if (loadPdfs) map.projection else map.imageProjection

        val northWest = projection.toPixels(bounds.northWest)
        val southEast = projection.toPixels(bounds.southEast)
        val southWest = projection.toPixels(bounds.southWest)
        val northEast = projection.toPixels(bounds.northEast)

        val rawSize = if (loadPdfs) {
            map.metadata.size
        } else {
            map.metadata.imageSize
        }
        val warpedSize = map.unrotatedSize(loadPdfs)
        val sourceTransform = getSourceTransform(rawSize, warpedSize)

        val virtualCorners = PhotoMapWarp.Corners(northWest, northEast, southWest, southEast)
        val sourceCorners = sourceTransform?.let {
            PhotoMapWarp.map(it.matrix, virtualCorners)
        } ?: virtualCorners

        val region = BitmapUtils.getExactRegion(
            PhotoMapWarp.boundingRect(sourceCorners),
            rawSize.toAndroidSize()
        )
        if (region.width() <= 0 || region.height() <= 0) {
            return@onIO null // No area to load
        }

        val perspectiveBounds = PhotoMapWarp.perspectiveBounds(sourceCorners, region)

        val shouldApplyPerspectiveCorrection = listOf(
            perspectiveBounds.topLeft.x,
            perspectiveBounds.topLeft.y,
            perspectiveBounds.bottomRight.x,
            perspectiveBounds.bottomRight.y,
            perspectiveBounds.topRight.x,
            perspectiveBounds.topRight.y,
            perspectiveBounds.bottomLeft.x,
            perspectiveBounds.bottomLeft.y
        ).any { !Arithmetic.isZero(it % 1f) } || sourceTransform != null

        val inSampleSize = calculateInSampleSize(
            region.width(),
            region.height(),
            (maxSize.width * sampleSizeMultiplier).roundToInt(),
            (maxSize.height * sampleSizeMultiplier).roundToInt()
        )

        val isPdf = loadPdfs && map.hasPdf(context)
        val bitmap = decoderCache.decodeRegion(context, map, region, inSampleSize, isPdf)

        bitmap?.applyOperationsOrNull(
            Conditional(
                shouldApplyPerspectiveCorrection,
                CorrectPerspective(
                    perspectiveBounds,
                    maxSize = maxSize,
                    outputSize = maxSize,
                    interpolate = !isPixelPerfect
                )
            ),
            Resize(maxSize, true, useBilinearScaling = !isPixelPerfect),
            *operations.toTypedArray()
        )?.let {
            cropToWarpedBounds(
                it,
                sourceTransform,
                warpedSize,
                virtualCorners
            )
        }
    }

    private fun cropToWarpedBounds(
        bitmap: Bitmap,
        sourceTransform: SourceTransform?,
        warpedSize: Size,
        virtualCorners: PhotoMapWarp.Corners
    ): Bitmap {
        if (sourceTransform == null) {
            return bitmap
        }

        return PhotoMapWarp.featherCrop(bitmap, warpedSize, virtualCorners)
    }

    private fun getSourceTransform(rawSize: Size, warpedSize: Size): SourceTransform? {
        val warpBounds = map.calibration.warpBounds ?: return null
        if (!map.calibration.warped) {
            return null
        }

        return SourceTransform(PhotoMapWarp.sourceTransform(rawSize, warpedSize, warpBounds) ?: return null)
    }

    private class SourceTransform(val matrix: Matrix)

    private fun calculateInSampleSize(
        sourceWidth: Int,
        sourceHeight: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        // Raw height and width of image
        var inSampleSize = 1

        if (sourceHeight > reqHeight || sourceWidth > reqWidth) {

            val halfHeight: Int = sourceHeight / 2
            val halfWidth: Int = sourceWidth / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}
