package com.kylecorry.trail_sense.tools.maps.infrastructure

import android.R.attr.bitmap
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
        width,
        height,
        null,
        true
    )
    resultBitmap.recycle()
    return newBitmap
}

fun Bitmap.rotate(angle: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(angle) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

object BitmapUtils2 {

    fun decodeBitmapScaled(
        path: String,
        maxWidth: Int,
        maxHeight: Int
    ): Bitmap {
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, this)
            inSampleSize = calculateInSampleSize(this, maxWidth, maxHeight)
            inJustDecodeBounds = false
            BitmapFactory.decodeFile(path, this)
        }
    }

    fun getBitmapSize(path: String): Pair<Int, Int> {
        val opts = BitmapFactory.Options().run {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, this)
            this
        }
        return Pair(opts.outWidth, opts.outHeight)
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        // Raw height and width of image
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

}