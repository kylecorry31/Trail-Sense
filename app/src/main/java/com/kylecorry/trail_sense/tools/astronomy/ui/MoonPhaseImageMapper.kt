package com.kylecorry.trail_sense.tools.astronomy.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.math.arithmetic.Arithmetic
import com.kylecorry.sol.math.trigonometry.Trigonometry.cosDegrees
import com.kylecorry.trail_sense.R
import kotlin.math.abs

class MoonPhaseImageMapper(private val context: Context) {

    private val moonDrawable by lazy {
        Resources.drawable(context, R.drawable.ic_moon)
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = SHADOW_COLOR
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }

    fun getPhaseImage(
        phaseAngle: Float,
        width: Int,
        height: Int,
        tilt: Float? = null
    ): Bitmap {
        val output = createBitmap(width, height)
        val canvas = Canvas(output)
        canvas.withSave {
            tilt?.let {
                rotate(it, width / 2f, height / 2f)
            }
            moonDrawable?.let {
                it.setBounds(0, 0, width, height)
                it.draw(this)
            }

            drawShadow(this, phaseAngle, width.toFloat(), height.toFloat(), shadowPaint)
        }
        return output
    }

    private fun drawShadow(
        canvas: Canvas,
        angle: Float,
        width: Float,
        height: Float,
        paint: Paint
    ) {
        when {
            Arithmetic.isZero(angle) || Arithmetic.isApproximatelyEqual(angle, 360f) -> {
                canvas.drawRect(0f, 0f, width, height, paint)
            }

            else -> {
                canvas.drawPath(createShadowPath(angle, width, height), paint)
            }
        }
    }

    private fun createShadowPath(angle: Float, width: Float, height: Float): Path {
        val centerX = width / 2f
        val terminatorRadius = abs(cosDegrees(angle)) * centerX
        val terminator = RectF(
            centerX - terminatorRadius,
            0f,
            centerX + terminatorRadius,
            height
        )
        val shadowOnLeft = angle < 180f
        val terminatorOnRight = (cosDegrees(angle) >= 0) == shadowOnLeft
        val offset = 0.5f
        return Path().apply {
            moveTo(centerX, -offset)
            lineTo(if (shadowOnLeft) -offset else width + offset, -offset)
            lineTo(if (shadowOnLeft) -offset else width + offset, height + offset)
            lineTo(centerX, height + offset)
            if (Arithmetic.isZero(terminatorRadius)) {
                lineTo(centerX, -offset)
            } else {
                arcTo(
                    terminator,
                    90f,
                    if (terminatorOnRight) -180f else 180f
                )
            }
            close()
        }
    }

    companion object {
        private const val SHADOW_COLOR = 0xE0000000.toInt()
    }
}
