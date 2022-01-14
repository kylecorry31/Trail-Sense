package com.kylecorry.trail_sense.tools.maps.infrastructure

import android.graphics.Bitmap
import android.graphics.Matrix
import com.kylecorry.trail_sense.tools.maps.domain.PixelBounds
import kotlin.math.max
import kotlin.math.roundToInt


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

// https://stackoverflow.com/questions/13161628/cropping-a-perspective-transformation-of-image-on-android
fun Bitmap.fixPerspective(bounds: PixelBounds): Bitmap {
    // TODO: Do this in a way which doesn't reduce the size of the source bitmap - maybe just save the transformations and apply them later (this would allow for recalibration)
    val top = bounds.topLeft.distanceTo(bounds.topRight)
    val bottom = bounds.bottomLeft.distanceTo(bounds.bottomRight)
    val newWidth = (top + bottom) / 2f

    val left = bounds.topLeft.distanceTo(bounds.bottomLeft)
    val right = bounds.topRight.distanceTo(bounds.bottomRight)
    val newHeight = (left + right) / 2f

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
            newWidth, 0f,
            newWidth, newHeight,
            0f, newHeight
        ),
        0,
        4
    )

    val mappedTL = floatArrayOf(0f, 0f)
    matrix.mapPoints(mappedTL)
    val maptlx = mappedTL[0].roundToInt()
    val maptly = mappedTL[1].roundToInt()

    val mappedTR = floatArrayOf(width.toFloat(), 0f)
    matrix.mapPoints(mappedTR)
    val maptry = mappedTR[1].roundToInt()

    val mappedLL = floatArrayOf(0f, height.toFloat())
    matrix.mapPoints(mappedLL)
    val mapllx = mappedLL[0].roundToInt()

    val shiftX = max(-maptlx, -mapllx)
    val shiftY = max(-maptry, -maptly)

    val resultBitmap: Bitmap =
        Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    val newBitmap = Bitmap.createBitmap(
        resultBitmap,
        shiftX,
        shiftY,
        newWidth.toInt(),
        newHeight.toInt(),
        null,
        true
    )
    if (resultBitmap != newBitmap) {
        resultBitmap.recycle()
    }
    return newBitmap
}

fun Bitmap.rotate(angle: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(angle) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}