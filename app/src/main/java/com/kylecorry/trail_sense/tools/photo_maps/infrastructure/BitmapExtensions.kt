package com.kylecorry.trail_sense.tools.photo_maps.infrastructure

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Size
import androidx.annotation.ColorInt
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.kylecorry.andromeda.bitmaps.BitmapUtils
import com.kylecorry.andromeda.core.math.MathUtils
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.tools.photo_maps.domain.PixelBounds

fun BitmapUtils.getExactRegion(rect: Rect, imageSize: Size, blockSize: Int = 16): Rect {
    val left = rect.left.coerceIn(0, imageSize.width)
    val top = rect.top.coerceIn(0, imageSize.height)
    val right = rect.right.coerceIn(0, imageSize.width)
    val bottom = rect.bottom.coerceIn(0, imageSize.height)

    // Align it to a 16 pixel block
    val alignedLeft = (left / blockSize) * blockSize
    val alignedTop = (top / blockSize) * blockSize
    val alignedRight = ((right + (blockSize - 1)) / blockSize) * blockSize
    val alignedBottom = ((bottom + (blockSize - 1)) / blockSize) * blockSize
    return Rect(
        alignedLeft.coerceIn(0, imageSize.width),
        alignedTop.coerceIn(0, imageSize.height),
        alignedRight.coerceIn(0, imageSize.width),
        alignedBottom.coerceIn(0, imageSize.height)
    )
}

// Don't allow concave polygons
fun Bitmap.fixPerspective(
    bounds: PixelBounds,
    shouldRecycleOriginal: Boolean = false,
    @ColorInt backgroundColor: Int? = null
): Bitmap {
    return fixPerspective2(
        bounds.topLeft,
        bounds.topRight,
        bounds.bottomLeft,
        bounds.bottomRight,
        shouldRecycleOriginal,
        backgroundColor
    )
}

fun Bitmap.resizeToFit2(maxWidth: Int, maxHeight: Int, useBilinearScaling: Boolean = true): Bitmap {
    return if (maxHeight > 0 && maxWidth > 0) {
        val scaledSize = MathUtils.scaleToBounds(Size(width, height), Size(maxWidth, maxHeight))
        this.scale(scaledSize.width, scaledSize.height, useBilinearScaling)
    } else {
        this
    }
}

fun Bitmap.fixPerspective2(
    topLeft: PixelCoordinate,
    topRight: PixelCoordinate,
    bottomLeft: PixelCoordinate,
    bottomRight: PixelCoordinate,
    shouldRecycleOriginal: Boolean = false,
    @ColorInt backgroundColor: Int? = null
): Bitmap {
    val top = topLeft.distanceTo(topRight)
    val bottom = bottomLeft.distanceTo(bottomRight)
    val newWidth = ((top + bottom) / 2f).coerceAtLeast(1f)

    val left = topLeft.distanceTo(bottomLeft)
    val right = topRight.distanceTo(bottomRight)
    val newHeight = ((left + right) / 2f).coerceAtLeast(1f)

    val matrix = Matrix()
    matrix.setPolyToPoly(
        floatArrayOf(
            topLeft.x, topLeft.y,
            topRight.x, topRight.y,
            bottomRight.x, bottomRight.y,
            bottomLeft.x, bottomLeft.y,
        ),
        0,
        floatArrayOf(
            0f, 0f,
            newWidth, 0f,
            newWidth, newHeight,
            0f, newHeight
        ),
        0,
        4
    )

    // Create an empty mutable bitmap
    val blank =
        createBitmap(newWidth.toInt(), newHeight.toInt(), config ?: Bitmap.Config.ARGB_8888)
    // Create a canvas to draw on
    val canvas = Canvas(blank)

    if (backgroundColor != null) {
        canvas.drawColor(backgroundColor)
    }

    // Apply matrix to canvas
    canvas.concat(matrix)

    canvas.drawBitmap(this, 0f, 0f, null)

    if (shouldRecycleOriginal) {
        this.recycle()
    }

    return blank
}