package com.kylecorry.trail_sense.navigation.ui

/**
 * Adapted from https://github.com/RedInput/CompassView
 *
 * View original license: https://github.com/RedInput/CompassView/blob/master/LICENSE
 */

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.toBitmap
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trailsensecore.domain.geo.Bearing
import com.kylecorry.trailsensecore.domain.geo.CompassDirection
import com.kylecorry.trailsensecore.domain.math.deltaAngle
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.roundToInt

class LinearCompassView: View, ICompassView {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private val formatService = FormatService(context)
    private lateinit var paint: Paint
    private val icons = mutableMapOf<Int, Bitmap>()
    private var indicators = listOf<BearingIndicator>()
    private var compass: Bitmap? = null
    private var isInit = false
    private var azimuth = Bearing(0f)

    var range = 180f

    private var iconSize = 0
    private var textSize = 0f


    override fun onDraw(canvas: Canvas) {
        if (!isInit) {
            paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.textAlign = Paint.Align.CENTER
            iconSize = dp(25f).toInt()
            textSize = sp(15f)
            paint.textSize = textSize
            val compassSize = min(height, width) - 2 * iconSize - 2 * dp(2f).toInt()
            isInit = true
            val compassDrawable = UiUtils.drawable(context, R.drawable.compass)
            compass = compassDrawable?.toBitmap(compassSize, compassSize)
        }
        if (visibility != VISIBLE){
            postInvalidateDelayed(20)
            invalidate()
            return
        }
        canvas.drawColor(Color.TRANSPARENT)

        drawAzimuth(canvas)
        drawCompass(canvas)
        drawBearings(canvas)
        postInvalidateDelayed(20)
        invalidate()
    }

    private fun drawBearings(canvas: Canvas) {
        val minDegrees = (azimuth.value - range / 2).roundToInt()
        val maxDegrees = (azimuth.value + range / 2).roundToInt()
        for (indicator in indicators) {
            paint.colorFilter = if (indicator.tint != null) {
                PorterDuffColorFilter(indicator.tint, PorterDuff.Mode.SRC_IN)
            } else {
                null
            }
            val delta = deltaAngle(azimuth.value.roundToInt().toFloat(), indicator.bearing.value.roundToInt().toFloat())
            val centerPixel = when {
                delta < -range / 2f -> {
                    0f // TODO: Display indicator that is off screen
                }
                delta > range / 2f -> {
                    width.toFloat() // TODO: Display indicator that is off screen
                }
                else -> {
                    val deltaMin = deltaAngle(indicator.bearing.value, minDegrees.toFloat()).absoluteValue / (maxDegrees - minDegrees).toFloat()
                    deltaMin * width
                }
            }
            paint.alpha = (255 * indicator.opacity).toInt()
            val bitmap = getBitmap(indicator.icon)
            canvas.drawBitmap(
                bitmap,
                centerPixel - iconSize / 2f,
                0f,
                paint
            )
        }
        paint.colorFilter = null
        paint.alpha = 255
    }

    private fun drawAzimuth(canvas: Canvas){
        paint.colorFilter = PorterDuffColorFilter(UiUtils.androidTextColorPrimary(context), PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(
            getBitmap(R.drawable.ic_arrow_target),
            width / 2f - iconSize / 2f,
            0f,
            paint
        )
        paint.colorFilter = null
    }


    private fun drawCompass(canvas: Canvas){
        val pixDeg = width / range
        val minDegrees = (azimuth.value - range / 2).roundToInt()
        val maxDegrees = (azimuth.value + range / 2).roundToInt()
        var i = -180
        while (i < 540) {
            if (i in minDegrees..maxDegrees) {
                when {
                    i % 45 == 0 -> {
                        paint.color = UiUtils.color(context, R.color.colorPrimary)
                        paint.strokeWidth = 8f
                    }
                    else -> {
                        paint.color = UiUtils.androidTextColorPrimary(context)
                        paint.strokeWidth = 8f
                    }
                }
                when {
                    i % 90 == 0 -> {
                        canvas.drawLine(
                            pixDeg * (i - minDegrees),
                            height.toFloat(),
                            pixDeg * (i - minDegrees),
                            0.5f * height,
                            paint
                        )
                        val coord = when (i) {
                            -90, 270 -> formatService.formatDirection(CompassDirection.West)
                            0, 360 -> formatService.formatDirection(CompassDirection.North)
                            90, 450 -> formatService.formatDirection(CompassDirection.East)
                            -180, 180 -> formatService.formatDirection(CompassDirection.South)
                            else -> ""
                        }
                        paint.color = UiUtils.androidTextColorPrimary(context)
                        canvas.drawText(coord, pixDeg * (i - minDegrees), 5 / 12f * height, paint)
                    }
                    i % 15 == 0 -> {
                        canvas.drawLine(
                            pixDeg * (i - minDegrees),
                            height.toFloat(),
                            pixDeg * (i - minDegrees),
                            0.75f * height,
                            paint
                        )
                    }
                    else -> {
                        canvas.drawLine(
                            pixDeg * (i - minDegrees),
                            height.toFloat(),
                            pixDeg * (i - minDegrees),
                            10 / 12f * height,
                            paint
                        )
                    }
                }
            }
            i += 5
        }
    }

    private fun getBitmap(@DrawableRes id: Int): Bitmap {
        val bitmap = if (icons.containsKey(id)) {
            icons[id]
        } else {
            val drawable = UiUtils.drawable(context, id)
            val bm = drawable?.toBitmap(iconSize, iconSize)
            icons[id] = bm!!
            icons[id]
        }
        return bitmap!!
    }

    override fun setAzimuth(azimuth: Bearing) {
        this.azimuth = azimuth
    }

    override fun setIndicators(indicators: List<BearingIndicator>) {
        this.indicators = indicators
    }

    private fun dp(size: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, size,
            resources.displayMetrics
        )
    }

    private fun sp(size: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, size,
            resources.displayMetrics
        )
    }
}