package com.kylecorry.trail_sense.shared.data

import android.graphics.Rect
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.andromeda_temp.heightDegrees
import com.kylecorry.trail_sense.shared.andromeda_temp.widthDegrees
import kotlin.math.floor

typealias FloatBitmap = Array<Array<FloatArray>>

class GeographicImageSource(
    private val reader: DataImageReader,
    private val bounds: CoordinateBounds = CoordinateBounds.world,
    private val precision: Int = 2,
    private val valuePixelOffset: Float = 0f,
    private val latitudePixelsPerDegreeOverride: Double? = null,
    private val longitudePixelsPerDegreeOverride: Double? = null,
    // TODO: All of these should be hidden from the geographic image source
    private val interpolate: Boolean = true,
    private val interpolationOrder: Int = 1
) {
    val imageSize = reader.getSize()

    fun getPixel(location: Coordinate): PixelCoordinate {
        val imageSize = reader.getSize()
        var x: Double
        var y: Double

        // TODO: These should be the same and no override should be needed
        if (!SolMath.isZero(valuePixelOffset)) {
            val horizontalRes = bounds.widthDegrees() / imageSize.width
            val verticalRes = bounds.heightDegrees() / imageSize.height
            x =
                (location.longitude - (bounds.west + horizontalRes * valuePixelOffset)) / horizontalRes
            y = ((bounds.north - verticalRes * valuePixelOffset) - location.latitude) / verticalRes
        } else {
            val latitudePixelsPerDegree: Double = latitudePixelsPerDegreeOverride
                ?: ((imageSize.height - 1) / bounds.heightDegrees())
            val longitudePixelsPerDegree: Double = longitudePixelsPerDegreeOverride
                ?: ((imageSize.width - 1) / bounds.widthDegrees())
            x = (location.longitude - bounds.west) * longitudePixelsPerDegree
            y = (bounds.north - location.latitude) * latitudePixelsPerDegree
        }

        if (x.isNaN()) {
            x = 0.0
        }

        if (y.isNaN()) {
            y = 0.0
        }
        return PixelCoordinate(
            x.roundPlaces(precision).toFloat().coerceIn(
                -valuePixelOffset,
                imageSize.width.toFloat() - 1f + valuePixelOffset
            ),
            y.roundPlaces(precision).toFloat().coerceIn(
                -valuePixelOffset,
                imageSize.height.toFloat() - 1f + valuePixelOffset
            )
        )
    }

    suspend fun read(location: Coordinate): List<Float> = onIO {
        read(getPixel(location))
    }

    suspend fun read(pixel: PixelCoordinate): List<Float> = onIO {
        read(listOf(pixel)).first().second
    }

    private fun getRect(pixels: List<PixelCoordinate>, interpolationOrder: Int): Rect? {
        if (pixels.isEmpty()) {
            return null
        }
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        for (pixel in pixels) {
            if (pixel.x < minX) {
                minX = pixel.x
            }
            if (pixel.x > maxX) {
                maxX = pixel.x
            }
            if (pixel.y < minY) {
                minY = pixel.y
            }
            if (pixel.y > maxY) {
                maxY = pixel.y
            }
        }
        return Rect(
            floor(minX).toInt() - interpolationOrder,
            floor(minY).toInt() - interpolationOrder,
            floor(maxX).toInt() + 1 + interpolationOrder,
            floor(maxY).toInt() + 1 + interpolationOrder
        )
    }

    suspend fun read(pixels: List<PixelCoordinate>): List<Pair<PixelCoordinate, List<Float>>> =
        onIO {
            // Divide the pixels into subregions of at most 255x255 pixels in the original image (using x, y)
            val regions = mutableMapOf<Pair<Int, Int>, MutableList<PixelCoordinate>>()
            for (pixel in pixels) {
                val regionX = (pixel.x / 255).toInt()
                val regionY = (pixel.y / 255).toInt()
                val regionKey = Pair(regionX, regionY)
                if (regionKey !in regions) {
                    regions[regionKey] = mutableListOf()
                }
                regions[regionKey]!!.add(pixel)
            }
            val results = mutableListOf<Pair<PixelCoordinate, List<Float>>>()
            val interpolators = listOfNotNull(
                if (interpolate && interpolationOrder == 2) BicubicInterpolator() else null,
                if (interpolate) BilinearInterpolator() else null,
                NearestInterpolator()
            )
            for (region in regions.values) {
                val rect = getRect(region, interpolationOrder) ?: continue
                val (pixelGrid, hasData) = reader.getRegion(rect) ?: continue
                for (pixel in region) {
                    val interpolated = mutableListOf<Float>()
                    for (i in 0 until pixelGrid[0][0].size) {
                        if (!hasData) {
                            interpolated.add(0f)
                            continue
                        }
                        val localPixel = PixelCoordinate(
                            pixel.x - rect.left,
                            pixel.y - rect.top
                        )
                        interpolated.add(interpolators.firstNotNullOfOrNull {
                            it.interpolate(localPixel, pixelGrid, i)
                        } ?: 0f)
                    }
                    results.add(pixel to interpolated)
                }
            }
            results
        }

    fun contains(location: Coordinate): Boolean {
        return bounds.contains(location)
    }
}