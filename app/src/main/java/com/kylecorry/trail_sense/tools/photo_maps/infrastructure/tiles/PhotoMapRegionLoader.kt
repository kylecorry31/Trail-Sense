package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.util.Size
import androidx.core.net.toUri
import com.kylecorry.andromeda.bitmaps.BitmapUtils
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.andromeda_temp.ImageRegionLoader
import com.kylecorry.trail_sense.shared.andromeda_temp.ceilToInt
import com.kylecorry.trail_sense.shared.andromeda_temp.floorToInt
import com.kylecorry.trail_sense.shared.bitmaps.BitmapOperation
import com.kylecorry.trail_sense.shared.bitmaps.Conditional
import com.kylecorry.trail_sense.shared.bitmaps.CorrectPerspective
import com.kylecorry.trail_sense.shared.bitmaps.ReplaceColor
import com.kylecorry.trail_sense.shared.bitmaps.Resize
import com.kylecorry.trail_sense.shared.bitmaps.applyOperations
import com.kylecorry.trail_sense.shared.canvas.tiles.PdfImageRegionDecoder
import com.kylecorry.trail_sense.shared.extensions.toAndroidSize
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.photo_maps.domain.PercentBounds
import com.kylecorry.trail_sense.tools.photo_maps.domain.PercentCoordinate
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.getExactRegion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PhotoMapRegionLoader(
    private val context: Context,
    val map: PhotoMap,
    private val replaceWhitePixels: Boolean = false,
    private val loadPdfs: Boolean = true,
    private val isPixelPerfect: Boolean = false,
    private val operations: List<BitmapOperation> = emptyList()
) : IGeographicImageRegionLoader {

    override suspend fun load(bounds: CoordinateBounds, maxSize: Size): Bitmap? = onIO {
        val fileSystem = AppServiceRegistry.get<FileSubsystem>()
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
        ).any { !SolMath.isZero(it % 1f) }

        val bitmap = if (loadPdfs && map.hasPdf(context)) {
            decodePdfRegion(
                context, map,
                region, calculateInSampleSize(
                    region.width(),
                    region.height(),
                    maxSize.width,
                    maxSize.height
                )
            )
        } else {
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
                    it.inMutable = true
                }

                ImageRegionLoader.decodeBitmapRegionWrapped(
                    stream,
                    region,
                    size.toAndroidSize(),
                    destinationSize = maxSize,
                    options = options,
                    enforceBounds = false
                )
            }
        }

        bitmap?.applyOperations(
            Resize(maxSize, false, useBilinearScaling = !isPixelPerfect),
            Conditional(
                shouldApplyPerspectiveCorrection,
                CorrectPerspective(
                    // Bounds are inverted on the Y axis from android's pixel coordinate system
                    PercentBounds(
                        percentBottomLeft,
                        percentBottomRight,
                        percentTopLeft,
                        percentTopRight
                    ),
                )
            ),
            Resize(maxSize, true, useBilinearScaling = !isPixelPerfect),
            Conditional(
                replaceWhitePixels,
                ReplaceColor(
                    Color.WHITE,
                    Color.argb(127, 127, 127, 127),
                    80f,
                    true,
                    inPlace = true
                )
            ),
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

    // TODO: Rather than PDF loaders, change this to cache the photo map region loader and add a recycle method
    companion object {
        val loaderLock = Any()
        val loaders = mutableMapOf<PhotoMap, PdfImageRegionDecoder>()
        private val scope = CoroutineScope(Dispatchers.IO)

        private fun getLoader(context: Context, map: PhotoMap): PdfImageRegionDecoder {
            synchronized(loaderLock) {
                if (!loaders.containsKey(map)) {
                    val decoder = PdfImageRegionDecoder(Bitmap.Config.ARGB_8888)
                    decoder.init(
                        context,
                        AppServiceRegistry.get<FileSubsystem>().get(map.pdfFileName).toUri()
                    )
                    loaders[map] = decoder
                }
                return loaders[map]!!
            }
        }

        fun removeUnneededLoaders(activeMaps: List<PhotoMap>) {
            scope.launch {
                synchronized(loaderLock) {
                    val loadersToRemove = loaders.keys.filter { it !in activeMaps }
                    for (map in loadersToRemove) {
                        loaders.remove(map)
                        loaders[map]?.recycle()
                    }
                }
            }
        }

        fun decodePdfRegion(
            context: Context,
            map: PhotoMap,
            region: Rect,
            sampleSize: Int
        ): Bitmap? {
            val loader = getLoader(context, map)
            return tryOrDefault(null) { loader.decodeRegion(region, sampleSize) }
        }
    }
}