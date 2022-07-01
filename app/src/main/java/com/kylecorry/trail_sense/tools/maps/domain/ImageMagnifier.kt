package com.kylecorry.trail_sense.tools.maps.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.geometry.Size

class ImageMagnifier(private val imageSize: Size, private val magnifierSize: Size) {

    private val paint = Paint()

    fun getMagnifierPosition(tapPosition: PixelCoordinate): PixelCoordinate {
        val x = if (tapPosition.x > imageSize.width / 2) {
            0f
        } else {
            imageSize.width - magnifierSize.width
        }

        return PixelCoordinate(x, 0f)
    }

    // TODO: Support zoom
    fun magnify(
        source: Bitmap,
        sourceCenter: PixelCoordinate,
        dest: Bitmap = Bitmap.createBitmap(
            magnifierSize.width.toInt(),
            magnifierSize.height.toInt(),
            Bitmap.Config.ARGB_8888
        )
    ): Bitmap {
        val canvas = Canvas(dest)
        canvas.drawColor(Color.BLACK)

        val leftOffset = -(sourceCenter.x - magnifierSize.width / 2f)
        val topOffset = -(sourceCenter.y - magnifierSize.height / 2f)

        canvas.drawBitmap(source, leftOffset, topOffset, paint)

        return dest
    }
}