package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.kylecorry.andromeda.bitmaps.BitmapUtils
import com.kylecorry.andromeda.bitmaps.operations.BitmapOperation
import com.kylecorry.andromeda.bitmaps.operations.Conditional
import com.kylecorry.andromeda.bitmaps.operations.CorrectPerspective
import com.kylecorry.andromeda.bitmaps.operations.Resize
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.andromeda.core.units.PercentBounds
import com.kylecorry.andromeda.core.units.PercentCoordinate
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.sol.math.arithmetic.Arithmetic
import com.kylecorry.sol.math.ceilToInt
import com.kylecorry.sol.math.floorToInt
import com.kylecorry.trail_sense.shared.extensions.toAndroidSize
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap
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

        val left = listOf(northWest.x, southWest.x, northEast.x, southEast.x).min().floorToInt()
        val right = listOf(northWest.x, southWest.x, northEast.x, southEast.x).max().ceilToInt()
        val top = listOf(northWest.y, southWest.y, northEast.y, southEast.y).min().floorToInt()
        val bottom = listOf(northWest.y, southWest.y, northEast.y, southEast.y).max().ceilToInt()

        val size = map.unrotatedSize(loadPdfs)

        val region =
            BitmapUtils.getExactRegion(Rect(left, top, right, bottom), size.toAndroidSize())
        if (region.width() <= 0 || region.height() <= 0) {
            return@onIO null // No area to load
        }

        val percentTopRight = PercentCoordinate(
            (northEast.x - region.left) / region.width(),
            (northEast.y - region.top) / region.height()
        )
        val percentTopLeft = PercentCoordinate(
            (northWest.x - region.left) / region.width(),
            (northWest.y - region.top) / region.height()
        )
        val percentBottomRight = PercentCoordinate(
            (southEast.x - region.left) / region.width(),
            (southEast.y - region.top) / region.height()
        )
        val percentBottomLeft = PercentCoordinate(
            (southWest.x - region.left) / region.width(),
            (southWest.y - region.top) / region.height()
        )

        val shouldApplyPerspectiveCorrection = listOf(
            percentTopLeft.x,
            percentTopLeft.y,
            percentBottomRight.x,
            percentBottomRight.y,
            percentTopRight.x,
            percentTopRight.y,
            percentBottomLeft.x,
            percentBottomLeft.y
        ).any { !Arithmetic.isZero(it % 1f) }

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
                    PercentBounds(
                        percentTopLeft,
                        percentTopRight,
                        percentBottomLeft,
                        percentBottomRight,
                    ),
                    maxSize = maxSize,
                    outputSize = maxSize,
                    interpolate = !isPixelPerfect
                )
            ),
            Resize(maxSize, true, useBilinearScaling = !isPixelPerfect),
            *operations.toTypedArray()
        )
    }

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
