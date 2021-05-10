package com.kylecorry.trail_sense.tools.maps.infrastructure

import android.graphics.Bitmap
import android.graphics.Matrix
import com.kylecorry.trail_sense.tools.maps.domain.MapPixelBounds

fun Bitmap.resize(maxWidth: Int, maxHeight: Int): Bitmap {
    return if (maxHeight > 0 && maxWidth > 0) {
        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()
        var finalWidth = maxWidth
        var finalHeight = maxHeight
        if (ratioMax > ratioBitmap) {
            finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
        } else {
            finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
        }
        Bitmap.createScaledBitmap(this, finalWidth, finalHeight, true)
    } else {
        this
    }
}

fun Bitmap.fixPerspective(bounds: MapPixelBounds): Bitmap {
    val matrix = Matrix()
    matrix.setPolyToPoly(
        floatArrayOf(
            bounds.topLeft.x, bounds.topLeft.y,
            bounds.topRight.x, bounds.topRight.y,
            bounds.bottomRight.x, bounds.bottomRight.y,
            bounds.bottomLeft.x, bounds.bottomLeft.y,
        ),
        0,
        floatArrayOf(
            0f, 0f,
            width.toFloat(), 0f,
            width.toFloat(), height.toFloat(),
            0f, height.toFloat()
        ),
        0,
        4
    )
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}