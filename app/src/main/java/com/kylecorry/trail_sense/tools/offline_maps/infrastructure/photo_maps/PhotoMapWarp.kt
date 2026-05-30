package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Size
import com.kylecorry.andromeda.core.units.PercentBounds
import com.kylecorry.andromeda.core.units.PercentCoordinate
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.ceilToInt
import com.kylecorry.sol.math.floorToInt
import com.kylecorry.sol.math.geometry.Size as SolSize
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PercentBounds as DomainPercentBounds

object PhotoMapWarp {

    fun sourceTransform(
        rawSize: SolSize,
        warpedSize: SolSize,
        bounds: DomainPercentBounds
    ): Matrix? {
        val pixelBounds = bounds.toPixelBounds(rawSize.width, rawSize.height)
        val matrix = Matrix()
        matrix.setPolyToPoly(
            floatArrayOf(
                pixelBounds.topLeft.x, pixelBounds.topLeft.y,
                pixelBounds.topRight.x, pixelBounds.topRight.y,
                pixelBounds.bottomRight.x, pixelBounds.bottomRight.y,
                pixelBounds.bottomLeft.x, pixelBounds.bottomLeft.y,
            ),
            0,
            floatArrayOf(
                0f, 0f,
                warpedSize.width, 0f,
                warpedSize.width, warpedSize.height,
                0f, warpedSize.height
            ),
            0,
            4
        )

        val inverse = Matrix()
        return if (matrix.invert(inverse)) inverse else null
    }

    fun map(matrix: Matrix, pixel: PixelCoordinate): PixelCoordinate {
        val points = floatArrayOf(pixel.x, pixel.y)
        matrix.mapPoints(points)
        return PixelCoordinate(points[0], points[1])
    }

    fun corners(rect: Rect): Corners {
        return Corners(
            PixelCoordinate(rect.left.toFloat(), rect.top.toFloat()),
            PixelCoordinate(rect.right.toFloat(), rect.top.toFloat()),
            PixelCoordinate(rect.left.toFloat(), rect.bottom.toFloat()),
            PixelCoordinate(rect.right.toFloat(), rect.bottom.toFloat())
        )
    }

    fun map(matrix: Matrix, corners: Corners): Corners {
        return Corners(
            map(matrix, corners.topLeft),
            map(matrix, corners.topRight),
            map(matrix, corners.bottomLeft),
            map(matrix, corners.bottomRight)
        )
    }

    fun boundingRect(corners: Corners): Rect {
        return Rect(
            listOf(corners.topLeft.x, corners.topRight.x, corners.bottomLeft.x, corners.bottomRight.x).min().floorToInt(),
            listOf(corners.topLeft.y, corners.topRight.y, corners.bottomLeft.y, corners.bottomRight.y).min().floorToInt(),
            listOf(corners.topLeft.x, corners.topRight.x, corners.bottomLeft.x, corners.bottomRight.x).max().ceilToInt(),
            listOf(corners.topLeft.y, corners.topRight.y, corners.bottomLeft.y, corners.bottomRight.y).max().ceilToInt()
        )
    }

    fun exactRegion(region: Rect, size: Size): Rect {
        return Rect(
            region.left.coerceIn(0, size.width),
            region.top.coerceIn(0, size.height),
            region.right.coerceIn(0, size.width),
            region.bottom.coerceIn(0, size.height)
        )
    }

    fun perspectiveBounds(corners: Corners, region: Rect): PercentBounds {
        return PercentBounds(
            PercentCoordinate(
                (corners.topLeft.x - region.left) / region.width(),
                (corners.topLeft.y - region.top) / region.height()
            ),
            PercentCoordinate(
                (corners.topRight.x - region.left) / region.width(),
                (corners.topRight.y - region.top) / region.height()
            ),
            PercentCoordinate(
                (corners.bottomLeft.x - region.left) / region.width(),
                (corners.bottomLeft.y - region.top) / region.height()
            ),
            PercentCoordinate(
                (corners.bottomRight.x - region.left) / region.width(),
                (corners.bottomRight.y - region.top) / region.height()
            )
        )
    }

    fun featherCrop(bitmap: Bitmap, warpedSize: SolSize, virtualCorners: Corners): Bitmap {
        val output = if (bitmap.config == Bitmap.Config.ARGB_8888 && bitmap.isMutable) {
            bitmap
        } else {
            bitmap.copy(Bitmap.Config.ARGB_8888, true).also { bitmap.recycle() }
        }

        val width = output.width.coerceAtLeast(1)
        val height = output.height.coerceAtLeast(1)
        val feather = maxOf(
            (virtualCorners.topLeft.distanceTo(virtualCorners.topRight) + virtualCorners.bottomLeft.distanceTo(virtualCorners.bottomRight)) / (2f * width),
            (virtualCorners.topLeft.distanceTo(virtualCorners.bottomLeft) + virtualCorners.topRight.distanceTo(virtualCorners.bottomRight)) / (2f * height),
            1f
        ) * 1.5f

        for (y in 0 until height) {
            val v = if (height == 1) 0f else y.toFloat() / (height - 1)
            for (x in 0 until width) {
                val u = if (width == 1) 0f else x.toFloat() / (width - 1)
                val virtual = interpolate(virtualCorners, u, v)
                val distanceToEdge = minOf(
                    virtual.x,
                    virtual.y,
                    warpedSize.width - virtual.x,
                    warpedSize.height - virtual.y
                )
                val alpha = (((distanceToEdge + feather) / (2f * feather)).coerceIn(0f, 1f) * 255).toInt()
                if (alpha == 0) {
                    output.setPixel(x, y, Color.TRANSPARENT)
                } else if (alpha < 255) {
                    val color = output.getPixel(x, y)
                    output.setPixel(
                        x,
                        y,
                        Color.argb(
                            (Color.alpha(color) * alpha) / 255,
                            Color.red(color),
                            Color.green(color),
                            Color.blue(color)
                        )
                    )
                }
            }
        }
        return output
    }

    private fun interpolate(corners: Corners, u: Float, v: Float): PixelCoordinate {
        val topX = corners.topLeft.x + (corners.topRight.x - corners.topLeft.x) * u
        val topY = corners.topLeft.y + (corners.topRight.y - corners.topLeft.y) * u
        val bottomX = corners.bottomLeft.x + (corners.bottomRight.x - corners.bottomLeft.x) * u
        val bottomY = corners.bottomLeft.y + (corners.bottomRight.y - corners.bottomLeft.y) * u
        return PixelCoordinate(
            topX + (bottomX - topX) * v,
            topY + (bottomY - topY) * v
        )
    }

    data class Corners(
        val topLeft: PixelCoordinate,
        val topRight: PixelCoordinate,
        val bottomLeft: PixelCoordinate,
        val bottomRight: PixelCoordinate
    )
}
