package com.kylecorry.trail_sense.tools.clock.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Adapted from https://www.ssaurel.com/blog/learn-to-draw-an-analog-clock-on-android-with-the-canvas-2d-api/

class ClockView : View {
    private var padding = 0
    private var fontSize = 0
    private val numeralSpacing = 0
    private var handTruncation = 0
    private var hourHandTruncation = 0
    private var radius = 0
    private lateinit var paint: Paint
    private var isInit = false
    private val numbers = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
    private val rect = Rect()

    var time: LocalTime = LocalTime.now()
    var use24Hours = true

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private fun initClock() {
        padding = numeralSpacing + 50
        fontSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 13f,
            resources.displayMetrics
        ).toInt()
        val min = min(height, width)
        radius = min / 2 - padding
        handTruncation = min / 20
        hourHandTruncation = min / 7
        paint = Paint()
        isInit = true
    }

    override fun onDraw(canvas: Canvas) {
        if (!isInit) {
            initClock()
        }
        canvas.drawColor(Color.TRANSPARENT)
        drawNumeral(canvas)
        drawHands(canvas)
        drawCenter(canvas)
        postInvalidateDelayed(20)
        invalidate()
    }

    private fun drawHand(canvas: Canvas, loc: Double, isHour: Boolean, strokeWidth: Float = 3f) {
        val angle = Math.PI * loc / 30 - Math.PI / 2
        val handRadius =
            if (isHour) radius - handTruncation - hourHandTruncation else radius - handTruncation
        if (isHour) {
            paint.color = Resources.color(context, R.color.brand_orange)
        } else {
            paint.color = Color.WHITE
        }
        paint.strokeWidth = strokeWidth
        canvas.drawLine(
            width / 2f, height / 2f,
            (width / 2 + cos(angle) * handRadius).toFloat(),
            (height / 2 + sin(angle) * handRadius).toFloat(),
            paint
        )
    }

    private fun drawHands(canvas: Canvas) {
        var hour = time.hour.toFloat()
        hour = if (hour > 12) hour - 12 else hour
        drawHand(
            canvas,
            (hour + time.minute / 60f + time.second / (60f * 60f)) * 5f.toDouble(),
            true,
            10f
        )
        drawHand(canvas, (time.minute + time.second / 60f).toDouble(), false, 5f)
        drawHand(canvas, (time.second + time.nano * 1e-9f).toDouble(), false, 3f)
    }

    private fun drawNumeral(canvas: Canvas) {
        paint.textSize = fontSize.toFloat()
        paint.color = Color.WHITE
        for (number in numbers) {
            val tmp = number.toString()
            paint.getTextBounds(tmp, 0, tmp.length, rect)
            val angle = Math.PI / 6 * (number - 3)
            val x = (width / 2 + cos(angle) * radius - rect.width() / 2).toInt()
            val y = (height / 2 + sin(angle) * radius + rect.height() / 2).toInt()
            canvas.drawText(tmp, x.toFloat(), y.toFloat(), paint)
        }

        if (use24Hours) {
            paint.textSize = fontSize.toFloat() * 0.6f
            for (number in numbers) {
                val n = 12 + number
                val tmp = n.toString()
                paint.getTextBounds(tmp, 0, tmp.length, rect)
                val angle = Math.PI / 6 * (number - 3)
                val x = (width / 2 + cos(angle) * (radius * 0.75) - rect.width() / 2).toInt()
                val y = (height / 2 + sin(angle) * (radius * 0.75) + rect.height() / 2).toInt()
                canvas.drawText(tmp, x.toFloat(), y.toFloat(), paint)
            }
        }
    }

    private fun drawCenter(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawCircle(width / 2.toFloat(), height / 2.toFloat(), 12f, paint)
    }
}