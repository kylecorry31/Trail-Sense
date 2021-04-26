package com.kylecorry.trail_sense.navigation.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.toBitmap
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.domain.geo.Bearing
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.math.deltaAngle
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import kotlin.math.*


class RoundCompassView : View, ICompassView {
    private lateinit var paint: Paint
    private val icons = mutableMapOf<Int, Bitmap>()
    private var indicators = listOf<BearingIndicator>()
    private var compass: Bitmap? = null
    private var isInit = false
    private var azimuth = 0f
    private var destination: Float? = null
    @ColorInt
    private var destinationColor: Int? = null

    private var iconSize = 0

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onDraw(canvas: Canvas) {
        if (visibility != VISIBLE) {
            return
        }
        if (!isInit) {
            paint = Paint()
            iconSize =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 24f,
                    resources.displayMetrics
                ).toInt()
            val compassSize = min(height, width) - 2 * iconSize - 2 * dp(2f).toInt()
            isInit = true
            val compassDrawable = UiUtils.drawable(context, R.drawable.compass)
            compass = compassDrawable?.toBitmap(compassSize, compassSize)
        }
        canvas.drawColor(Color.TRANSPARENT)
        drawAzimuth(canvas)
        canvas.save()
        canvas.rotate(-azimuth, width / 2f, height / 2f)
        drawCompass(canvas)
        drawBearings(canvas)
        drawDestination(canvas)
        canvas.restore()
    }

    private fun drawDestination(canvas: Canvas) {
        destination ?: return
        val color = destinationColor ?: UiUtils.color(context, R.color.colorPrimary)
        canvas.save()
        paint.color = color
        paint.alpha = 100
        val dp2 = dp(2f)
        canvas.drawArc(
            iconSize.toFloat() + dp2,
            iconSize.toFloat() + dp2,
            width - iconSize.toFloat() - dp2,
            height - iconSize.toFloat() - dp2,
            270f + azimuth,
            deltaAngle(azimuth, destination!!),
            true,
            paint
        )
        paint.alpha = 255
        canvas.restore()
    }

    override fun setAzimuth(azimuth: Float) {
        this.azimuth = azimuth
        invalidate()
    }

    override fun setLocation(location: Coordinate) {
        // Nothing
    }

    override fun setIndicators(indicators: List<BearingIndicator>) {
        this.indicators = indicators
        invalidate()
    }

    override fun setDestination(bearing: Float?, @ColorInt color: Int?) {
        destination = bearing
        destinationColor = color
        invalidate()
    }

    private fun drawAzimuth(canvas: Canvas) {
        paint.colorFilter =
            PorterDuffColorFilter(UiUtils.androidTextColorPrimary(context), PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(
            getBitmap(R.drawable.ic_arrow_target),
            width / 2f - iconSize / 2f,
            0f,
            paint
        )
        paint.colorFilter = null
    }

    private fun drawCompass(canvas: Canvas) {
        paint.alpha = 255
        canvas.drawBitmap(
            compass!!,
            iconSize.toFloat() + dp(2f),
            iconSize.toFloat() + dp(2f),
            paint
        )
    }

    private fun drawBearings(canvas: Canvas) {
        for (indicator in indicators) {
            paint.colorFilter = if (indicator.tint != null) {
                PorterDuffColorFilter(indicator.tint, PorterDuff.Mode.SRC_IN)
            } else {
                null
            }
            paint.alpha = (255 * indicator.opacity).toInt()
            canvas.save()
            canvas.rotate(indicator.bearing, width / 2f, height / 2f)
            val bitmap = getBitmap(indicator.icon)
            canvas.drawBitmap(
                bitmap,
                width / 2f - iconSize / 2f,
                0f,
                paint
            )
            canvas.restore()
        }
        paint.colorFilter = null
        paint.alpha = 255
    }

    override fun setDeclination(declination: Float) {
        // Do nothing for now
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

    private fun dp(size: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, size,
            resources.displayMetrics
        )
    }


}