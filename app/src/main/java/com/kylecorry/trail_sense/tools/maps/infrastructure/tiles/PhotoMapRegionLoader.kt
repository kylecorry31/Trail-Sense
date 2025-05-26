package com.kylecorry.trail_sense.tools.maps.infrastructure.tiles

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Size
import com.kylecorry.andromeda.bitmaps.BitmapUtils
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import kotlin.math.max
import kotlin.math.min

class PhotoMapRegionLoader(private val map: PhotoMap) {

    suspend fun load(bounds: CoordinateBounds, maxSize: Size? = null): Bitmap? {
        // TODO: Map rotation (get the rotated area and crop?)
        val fileSystem = AppServiceRegistry.get<FileSubsystem>()
        val projection = map.projection

        val west = projection.toPixels(bounds.northWest)
            .x.toInt()
        val east = projection.toPixels(bounds.southEast)
            .x.toInt()
        val north = projection.toPixels(bounds.northWest)
            .y.toInt()
        val south = projection.toPixels(bounds.southEast)
            .y.toInt()

        val region = Rect(
            min(west, east),
            min(north, south),
            max(west, east),
            max(north, south)
        )
        return fileSystem.streamLocal(map.filename).use { stream ->
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
                }
            }
            BitmapUtils.decodeRegion(stream, region, options)
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