package com.kylecorry.trail_sense.shared.data

import android.graphics.Rect
import com.kylecorry.andromeda.bitmaps.FloatBitmap
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Coordinate
import kotlin.math.roundToInt

class CrossBoundaryPixelProvider(
    private val primarySource: GeographicImageSource,
    private val primaryBitmap: FloatBitmap,
    private val primaryRect: Rect,
    // TODO: This can be improved by preloading the neighbor regions that are needed
    private val neighborSources: List<GeographicImageSource>
) {
    private val neighborCache =
        mutableMapOf<GeographicImageSource, MutableList<Pair<FloatBitmap, Rect>>>()

    suspend fun getPixel(x: Int, y: Int, channel: Int): Float? {
        val localX = x - primaryRect.left
        val localY = y - primaryRect.top

        if (localX in 0 until primaryBitmap.width &&
            localY in 0 until primaryBitmap.height
        ) {
            val value = primaryBitmap.get(localX, localY, channel)
            if (!value.isNaN()) {
                return value
            }
        }

        val pixel = PixelCoordinate(x.toFloat(), y.toFloat())
        val location = primarySource.getLocation(pixel)

        for (neighbor in neighborSources) {
            if (!neighbor.contains(location)) {
                continue
            }

            val cachedValue = getFromCache(neighbor, location, channel)
            if (cachedValue != null) {
                return cachedValue
            }

            val regionData = loadNeighborRegion(neighbor, location) ?: continue
            val value = getValueFromRegion(neighbor, regionData, location, channel)
            if (value != null && !value.isNaN()) {
                return value
            }
        }

        return null
    }

    private fun getFromCache(
        neighbor: GeographicImageSource,
        location: Coordinate,
        channel: Int
    ): Float? {
        val regions = neighborCache[neighbor] ?: return null
        for ((bitmap, rect) in regions) {
            val value = getValueFromRegion(neighbor, bitmap to rect, location, channel)
            if (value != null && !value.isNaN()) {
                return value
            }
        }
        return null
    }

    private fun getValueFromRegion(
        neighbor: GeographicImageSource,
        regionData: Pair<FloatBitmap, Rect>,
        location: Coordinate,
        channel: Int
    ): Float? {
        val (bitmap, rect) = regionData
        val neighborPixel = neighbor.getPixel(location, clamp = false)
        val localX = neighborPixel.x.roundToInt() - rect.left
        val localY = neighborPixel.y.roundToInt() - rect.top

        if (localX in 0 until bitmap.width && localY in 0 until bitmap.height) {
            return bitmap.get(localX, localY, channel)
        }
        return null
    }

    private suspend fun loadNeighborRegion(
        source: GeographicImageSource,
        location: Coordinate
    ): Pair<FloatBitmap, Rect>? {
        val pixel = source.getPixel(location, clamp = false)
        val imageSize = source.imageSize
        val padding = 8

        val centerX = pixel.x.roundToInt()
        val centerY = pixel.y.roundToInt()
        val left = (centerX - padding).coerceAtLeast(0)
        val top = (centerY - padding).coerceAtLeast(0)
        val right = (centerX + padding + 1).coerceAtMost(imageSize.width)
        val bottom = (centerY + padding + 1).coerceAtMost(imageSize.height)

        if (right <= left || bottom <= top) {
            return null
        }

        val rect = Rect(left, top, right, bottom)
        val result = source.getRegion(rect) ?: return null
        val regionData = result.first to rect
        neighborCache.getOrPut(source) { mutableListOf() }.add(regionData)

        return regionData
    }
}
