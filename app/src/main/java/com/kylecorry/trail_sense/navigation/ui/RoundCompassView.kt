package com.kylecorry.trail_sense.navigation.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.toBitmap
import androidx.core.math.MathUtils
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.domain.light.LightIntensity
import com.kylecorry.trailsensecore.domain.units.Distance
import com.kylecorry.trailsensecore.domain.units.DistanceUnits
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import kotlin.math.*


class RoundCompassView : View {
    private lateinit var paint: Paint
    private val icons = mutableMapOf<Pair<Int, Float>, Bitmap>()
    private var indicators = listOf<BearingIndicator>()
    private var compass: Bitmap? = null
    private var isInit = false

    private var iconSize = 0

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onDraw(canvas: Canvas) {
        if (!isInit) {
            paint = Paint()
            iconSize =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 32f,
                    resources.displayMetrics
                ).toInt()
            val compassSize = min(height, width) - 2 * iconSize
            isInit = true
            val compassDrawable = UiUtils.drawable(context, R.drawable.compass)
            compass = compassDrawable?.toBitmap(compassSize, compassSize)
        }
        canvas.drawColor(Color.TRANSPARENT)
        drawCompass(canvas)
        drawBearings(canvas)
        postInvalidateDelayed(20)
        invalidate()
    }

    fun setIndicators(indicators: List<BearingIndicator>) {
        this.indicators = indicators
    }

    private fun drawCompass(canvas: Canvas) {
        paint.alpha = 255
        canvas.drawBitmap(compass!!, iconSize.toFloat(), iconSize.toFloat(), paint)
        canvas.save()
        canvas.rotate(-rotation, width / 2f, height / 2f)
        canvas.drawBitmap(getBitmap(R.drawable.ic_arrow_bearing, 50f), width / 2f - dp(25f), -dp(10f), paint)
        canvas.restore()
    }

    private fun drawBearings(canvas: Canvas) {
        for (indicator in indicators) {
            paint.color = indicator.tint ?: Color.TRANSPARENT
            paint.alpha = (255 * indicator.opacity).toInt()
            canvas.save()
            canvas.rotate(indicator.bearing.value, width / 2f, height / 2f)
            val bitmap = getBitmap(indicator.icon, indicator.size)
            canvas.drawBitmap(bitmap, width / 2f - dp(indicator.size) / 2f, dp(indicator.verticalOffset), paint)
            canvas.restore()
        }
    }

    private fun getBitmap(@DrawableRes id: Int, size: Float): Bitmap {
        val bitmap = if (icons.containsKey(id to size)) {
            icons[id to size]
        } else {
            val drawable = UiUtils.drawable(context, id)
            val bm = drawable?.toBitmap(dp(size).toInt(), dp(size).toInt())
            icons[id to size] = bm!!
            icons[id to size]
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