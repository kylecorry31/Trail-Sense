package com.kylecorry.trail_sense.tools.photo_maps.infrastructure

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.Size
import androidx.annotation.ColorInt
import androidx.core.graphics.createBitmap
import com.kylecorry.andromeda.core.math.MathUtils
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.tools.photo_maps.domain.PixelBounds

// Don't allow concave polygons
fun Bitmap.fixPerspective(
    bounds: PixelBounds,
    shouldRecycleOriginal: Boolean = false,
    @ColorInt backgroundColor: Int? = null,
    maxOutputSize: Size? = null
): Bitmap {
    return fixPerspective2(
        bounds.topLeft,
        bounds.topRight,
        bounds.bottomLeft,
        bounds.bottomRight,
        shouldRecycleOriginal,
        backgroundColor,
        maxOutputSize,
    )
}

fun Bitmap.fixPerspective2(
    topLeft: PixelCoordinate,
    topRight: PixelCoordinate,
    bottomLeft: PixelCoordinate,
    bottomRight: PixelCoordinate,
    shouldRecycleOriginal: Boolean = false,
    @ColorInt backgroundColor: Int? = null,
    maxOutputSize: Size? = null
): Bitmap {
    val top = topLeft.distanceTo(topRight)
    val bottom = bottomLeft.distanceTo(bottomRight)
    var newWidth = ((top + bottom) / 2f).coerceAtLeast(1f)

    val left = topLeft.distanceTo(bottomLeft)
    val right = topRight.distanceTo(bottomRight)
    var newHeight = ((left + right) / 2f).coerceAtLeast(1f)

    if (maxOutputSize != null && (newWidth > maxOutputSize.width || newHeight > maxOutputSize.height)) {
        val scale = MathUtils.scaleToBounds(
            Size(newWidth.toInt(), newHeight.toInt()),
            maxOutputSize
        )
        newWidth = scale.width.toFloat()
        newHeight = scale.height.toFloat()
    }

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