package com.kylecorry.trail_sense.tools.maps.infrastructure.tiles

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.util.Size
import com.kylecorry.andromeda.bitmaps.BitmapUtils.replaceColor
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.andromeda_temp.ImageRegionLoader
import com.kylecorry.trail_sense.shared.extensions.toAndroidSize
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.maps.domain.PixelBounds
import com.kylecorry.trail_sense.tools.maps.infrastructure.fixPerspective

class PhotoMapRegionLoader(private val map: PhotoMap) {

    suspend fun load(
        tile: Tile,
        maxSize: Size? = null,
        replaceWhitePixels: Boolean = false
    ): Bitmap? {
        return load(tile.getBounds(), maxSize, replaceWhitePixels)
    }

    suspend fun load(
        bounds: CoordinateBounds,
        maxSize: Size? = null,
        replaceWhitePixels: Boolean = false
    ): Bitmap? = onIO {
        // TODO: Map rotation (get the rotated area and crop?)
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

        // TODO: Load PDF region
        fileSystem.streamLocal(map.filename).use { stream ->
            val options = BitmapFactory.Options().also {
                if (maxSize != null) {
                    it.inSampleSize = calculateInSampleSize(
                        region.width(),
                        region.height(),
                        maxSize.width,
                        maxSize.height
                    )
                    it.inScaled = true
                    it.inPreferredConfig = Bitmap.Config.RGB_565
                    it.inMutable = replaceWhitePixels
                }
            }
            if (region.width() <= 0 || region.height() <= 0) {
                return@use null // No area to load
            }

            val bitmap = ImageRegionLoader.decodeBitmapRegionWrapped(
                stream,
                region,
                size.toAndroidSize(),
                options = options,
                enforceBounds = false
            )

            val rotated =
                if (SolMath.isZero(
                        SolMath.deltaAngle(map.calibration.rotation, 0f),
                        tolerance = 0.5f
                    )
                ) {
                    bitmap
                } else {
                    bitmap.fixPerspective(
                        PixelBounds(
                            // Bounds are inverted on the Y axis from android's pixel coordinate system
                            PixelCoordinate(
                                southWest.x - region.left,
                                southWest.y - region.top
                            ),
                            PixelCoordinate(
                                southEast.x - region.left,
                                southEast.y - region.top
                            ),
                            PixelCoordinate(
                                northWest.x - region.left,
                                northWest.y - region.top
                            ),
                            PixelCoordinate(
                                northEast.x - region.left,
                                northEast.y - region.top
                            )
                        ),
                        shouldRecycleOriginal = true
                    )

                }

            if (!replaceWhitePixels) {
                return@use rotated
            }

            rotated.replaceColor(Color.WHITE, Color.TRANSPARENT, 60f, true, inPlace = true)
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