package com.kylecorry.trail_sense.tools.tides.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.domain.oceanography.Tide
import com.kylecorry.trailsensecore.domain.oceanography.TideType
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Adapted from https://www.ssaurel.com/blog/learn-to-draw-an-analog-clock-on-android-with-the-canvas-2d-api/

class TideClockView : View {
    private var padding = 0
    private var fontSize = 0
    private val numeralSpacing = 0
    private var handTruncation = 0
    private var radius = 0
    private lateinit var paint: Paint
    private var clockColor = Color.BLACK
    private var textColor = Color.BLACK
    private var accentColor = Color.BLACK
    private var isInit = false
    private var labels: List<String> = listOf()
    private val rect = Rect()

    var time: ZonedDateTime = ZonedDateTime.now()
    var nextTide: Tide? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private fun initClock() {
        labels = listOf(
            "5",
            "4",
            "3",
            "2",
            "1",
            context.getString(R.string.low),
            "5",
            "4",
            "3",
            "2",
            "1",
            context.getString(
                R.string.high
            )
        )
        textColor = Color.WHITE
        accentColor = Resources.color(context, R.color.colorAccent)
        clockColor = Resources.color(context, R.color.colorSecondary)
        padding = numeralSpacing + 50
        fontSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 13f,
            resources.displayMetrics
        ).toInt()
        val min = min(height, width)
        radius = min / 2 - padding
        handTruncation = min / 12
        paint = Paint()
        isInit = true
    }

    override fun onDraw(canvas: Canvas) {
        if (!isInit) {
            initClock()
        }
        canvas.drawColor(Color.TRANSPARENT)
        drawBackground(canvas)
        drawLabels(canvas)
        drawHands(canvas)
        drawCenter(canvas)
        postInvalidateDelayed(20)
        invalidate()
    }

    private fun drawBackground(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = clockColor
        canvas.drawCircle(
            width / 2.toFloat(),
            height / 2.toFloat(),
            radius.toFloat() + padding,
            paint)
    }

    private fun drawHand(canvas: Canvas, loc: Double, strokeWidth: Float = 3f) {
        val angle = Math.PI * loc / 30 - Math.PI / 2
        val handRadius = radius - handTruncation
        paint.color = accentColor
        paint.strokeWidth = strokeWidth
        canvas.drawLine(
            width / 2f, height / 2f,
            (width / 2 + cos(angle) * handRadius).toFloat(),
            (height / 2 + sin(angle) * handRadius).toFloat(),
            paint
        )
    }

    private fun drawHands(canvas: Canvas) {
        nextTide ?: return
        val lunarDay = Duration.ofHours(24).plusMinutes(50).plusSeconds(30)
        val halfLunar = lunarDay.dividedBy(2)
        val timeUntilNextTide = Duration.between(time, nextTide!!.time)

        val percentDuration =
            (0.5f - timeUntilNextTide.seconds / halfLunar.seconds.toFloat()) + if (nextTide!!.type == TideType.High) 0.5f else 0f
        drawHand(
            canvas,
            percentDuration * 12.0 * 5.0,
            10f
        )
    }

    private fun drawLabels(canvas: Canvas) {
        paint.textSize = fontSize.toFloat()
        paint.color = textColor
        for (i in labels.indices) {
            val tmp = labels[i]
            if (i == 5 || i == labels.lastIndex){
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = accentColor
            } else {
                paint.typeface = Typeface.DEFAULT
                paint.color = textColor
            }
            paint.getTextBounds(tmp, 0, tmp.length, rect)
            val angle = Math.PI / 6 * (i - 2)
            val x = (width / 2 + cos(angle) * radius - rect.width() / 2).toInt()
            val y = (height / 2 + sin(angle) * radius + rect.height() / 2).toInt()
            canvas.drawText(tmp, x.toFloat(), y.toFloat(), paint)
        }
    }

    private fun drawCenter(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = textColor
        canvas.drawCircle(width / 2.toFloat(), height / 2.toFloat(), 12f, paint)
    }
}