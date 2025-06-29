package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.util.Size
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.andromeda_temp.ImageRegionLoader
import com.kylecorry.trail_sense.shared.bitmaps.Conditional
import com.kylecorry.trail_sense.shared.bitmaps.CorrectPerspective
import com.kylecorry.trail_sense.shared.bitmaps.ReplaceColor
import com.kylecorry.trail_sense.shared.bitmaps.Resize
import com.kylecorry.trail_sense.shared.bitmaps.applyOperations
import com.kylecorry.trail_sense.shared.extensions.toAndroidSize
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.photo_maps.domain.PercentBounds
import com.kylecorry.trail_sense.tools.photo_maps.domain.PercentCoordinate
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap

class PhotoMapRegionLoader(
    private val map: PhotoMap,
    private val replaceWhitePixels: Boolean = false
) : IGeographicImageRegionLoader {

    override suspend fun load(bounds: CoordinateBounds, maxSize: Size): Bitmap? = onIO {
        val fileSystem = AppServiceRegistry.get<FileSubsystem>()
        val projection = map.projection

        val northWest = projection.toPixels(bounds.northWest)
        val southEast = projection.toPixels(bounds.southEast)
        val southWest = projection.toPixels(bounds.southWest)
        val northEast = projection.toPixels(bounds.northEast)

        val left = listOf(northWest.x, southWest.x, northEast.x, southEast.x).min().toInt()
        val right = listOf(northWest.x, southWest.x, northEast.x, southEast.x).max().toInt()
        val top = listOf(northWest.y, southWest.y, northEast.y, southEast.y).min().toInt()
        val bottom = listOf(northWest.y, southWest.y, northEast.y, southEast.y).max().toInt()

        val size = map.metadata.unscaledPdfSize ?: map.metadata.size

        val region = Rect(left, top, right, bottom)

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

        val isRotated = !SolMath.isZero(
            SolMath.deltaAngle(map.calibration.rotation, 0f),
            0.5f
        )


        // TODO: Load PDF region
        val inputStream = if (map.isAsset) {
            fileSystem.streamAsset(map.filename)!!
        } else {
            fileSystem.streamLocal(map.filename)
        }

        inputStream.use { stream ->
            val options = BitmapFactory.Options().also {
                it.inSampleSize = calculateInSampleSize(
                    region.width(),
                    region.height(),
                    maxSize.width,
                    maxSize.height
                )
                it.inScaled = true
                it.inPreferredConfig = Bitmap.Config.ARGB_8888
                it.inMutable = replaceWhitePixels
            }
            if (region.width() <= 0 || region.height() <= 0) {
                return@use null // No area to load
            }

            val bitmap = ImageRegionLoader.decodeBitmapRegionWrapped(
                stream,
                region,
                size.toAndroidSize(),
                destinationSize = maxSize,
                options = options,
                enforceBounds = false
            )

            bitmap.applyOperations(
                Resize(maxSize, false),
                Conditional(
                    isRotated,
                    CorrectPerspective(
                        // Bounds are inverted on the Y axis from android's pixel coordinate system
                        PercentBounds(
                            percentBottomLeft,
                            percentBottomRight,
                            percentTopLeft,
                            percentTopRight
                        )
                    )
                ),
                Resize(maxSize, true),
                Conditional(
                    replaceWhitePixels,
                    ReplaceColor(
                        Color.WHITE,
                        Color.argb(127, 127, 127, 127),
                        80f,
                        true,
                        inPlace = true
                    )
                )
            )

        }
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