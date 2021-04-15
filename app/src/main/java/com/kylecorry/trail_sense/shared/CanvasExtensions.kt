package com.kylecorry.trail_sense.shared

import android.graphics.*

inline fun Canvas.getMaskedBitmap(mask: Bitmap,
                                  tempBitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888),
                                  block: (canvas: Canvas) -> Unit): Bitmap {
    val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    val tempCanvas = Canvas(tempBitmap)
    tempCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.DST_IN)
    block(tempCanvas)
    tempCanvas.drawBitmap(mask, 0f, 0f, maskPaint)
    return tempBitmap
}

class DottedPathEffect(size: Float = 3f, advance: Float = 10f, phase: Float = 0f): PathDashPathEffect(getDotPath(size), advance, phase, Style.ROTATE) {
    companion object {
        private fun getDotPath(size: Float): Path {
            val path = Path()
            path.addCircle(0f, 0f, 3f, Path.Direction.CW)
            return path
        }
    }
}