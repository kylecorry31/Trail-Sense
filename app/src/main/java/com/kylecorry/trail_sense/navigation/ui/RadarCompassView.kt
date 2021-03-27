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
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trailsensecore.domain.geo.Bearing
import com.kylecorry.trailsensecore.domain.math.deltaAngle
import com.kylecorry.trailsensecore.domain.units.Distance
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import kotlin.math.*


class RadarCompassView : View, ICompassView {
    private lateinit var paint: Paint
    private val icons = mutableMapOf<Int, Bitmap>()
    private var indicators = listOf<BearingIndicator>()
    private var compass: Bitmap? = null
    private var isInit = false
    private var azimuth = Bearing(0f)
    private var destination: Bearing? = null
    @ColorInt
    private var destinationColor: Int? = null

    private val prefs by lazy { UserPreferences(context) }
    @ColorInt
    private var primaryColor: Int = Color.WHITE
    @ColorInt
    private var secondaryColor: Int = Color.WHITE
    private val formatService by lazy { FormatServiceV2(context) }

    private var iconSize = 0
    private var radarSize = 0
    private var directionSize = 0
    private var compassSize = 0
    private var distanceSize = 0f
    private var cardinalSize = 0f
    private val rect = Rect()

    private var north = ""
    private var south = ""
    private var east = ""
    private var west = ""

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onDraw(canvas: Canvas) {
        if (!isInit) {
            paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.textAlign = Paint.Align.CENTER
            iconSize = dp(24f).toInt()
            radarSize = dp(10f).toInt()
            directionSize = dp(16f).toInt()
            compassSize = min(height, width) - 2 * iconSize - 2 * dp(2f).toInt()
            isInit = true
            distanceSize = sp(8f)
            cardinalSize = sp(10f)
            primaryColor = UiUtils.color(context, R.color.colorPrimary)
            secondaryColor = UiUtils.color(context, R.color.colorSecondary)
            val compassDrawable = UiUtils.drawable(context, R.drawable.radar)
            compass = compassDrawable?.toBitmap(compassSize, compassSize)
            north = context.getString(R.string.direction_north)
            south = context.getString(R.string.direction_south)
            east = context.getString(R.string.direction_east)
            west = context.getString(R.string.direction_west)
        }
        if (visibility != VISIBLE) {
            postInvalidateDelayed(20)
            invalidate()
            return
        }
        canvas.drawColor(Color.TRANSPARENT)
        canvas.save()
        canvas.rotate(-azimuth.value, width / 2f, height / 2f)
        drawCompass(canvas)
        drawBearings(canvas)
        drawDestination(canvas)
        canvas.restore()
        postInvalidateDelayed(20)
        invalidate()
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
            270f + azimuth.value,
            deltaAngle(azimuth.value, destination!!.value),
            true,
            paint
        )
        paint.alpha = 255
        canvas.restore()
    }

    override fun setAzimuth(bearing: Bearing) {
        azimuth = bearing
    }

    override fun setIndicators(indicators: List<BearingIndicator>) {
        this.indicators = indicators
    }

    override fun setDestination(bearing: Bearing?, @ColorInt color: Int?) {
        destination = bearing
        destinationColor = color
    }

    private fun drawCompass(canvas: Canvas) {
        paint.alpha = 255
        canvas.drawBitmap(
            compass!!,
            iconSize.toFloat() + dp(2f),
            iconSize.toFloat() + dp(2f),
            paint
        )
        paint.style = Paint.Style.STROKE
        paint.color = Color.WHITE
        paint.strokeWidth = 3f
        canvas.save()
        canvas.rotate(azimuth.value, width / 2f, height / 2f)
        canvas.drawBitmap(
            getBitmap(R.drawable.ic_beacon, directionSize), width / 2f - directionSize / 2f,
            height / 2f - directionSize / 2f,
            paint
        )
        paint.alpha = 127
        canvas.drawCircle(width / 2f, height / 2f, compassSize / 4f, paint)
        canvas.drawCircle(width / 2f, height / 2f, 3 * compassSize / 8f, paint)
        canvas.drawCircle(width / 2f, height / 2f, compassSize / 8f, paint)
        paint.alpha = 255
        paint.style = Paint.Style.FILL
        val maxDistance =
            Distance.meters(prefs.navigation.maxBeaconDistance).convertTo(prefs.baseDistanceUnits)
        val quarterDist = maxDistance.times(0.25f).toRelativeDistance()
        val threeQuarterDist = maxDistance.times(0.75f).toRelativeDistance()
        paint.textSize = distanceSize

        val quarterText = formatService.formatDistance(
            quarterDist,
            if (IsLargeUnitSpecification().isSatisfiedBy(quarterDist.units)) 1 else 0
        )
        val threeQuarterText = formatService.formatDistance(
            threeQuarterDist,
            if (IsLargeUnitSpecification().isSatisfiedBy(quarterDist.units)) 1 else 0
        )
        paint.getTextBounds(quarterText, 0, quarterText.length, rect)
        canvas.drawText(
            quarterText,
            width / 2f + compassSize / 8f,
            height / 2f + rect.height() / 2f,
            paint
        )
        paint.getTextBounds(threeQuarterText, 0, threeQuarterText.length, rect)
        canvas.drawText(
            threeQuarterText,
            width / 2f + 3 * compassSize / 8f,
            height / 2f + rect.height() / 2f,
            paint
        )
        canvas.restore()

        paint.textSize = cardinalSize
        canvas.save()
        canvas.rotate(0f, width / 2f, height / 2f)
        paint.getTextBounds(north, 0, north.length, rect)
        paint.color = secondaryColor
        canvas.drawRect(
            width / 2f - rect.width() / 2f - 8f,
            height / 2f - compassSize / 4f + rect.height() / 2f,
            width / 2f + rect.width() / 2f + 8f,
            height / 2f - compassSize / 4f - rect.height() / 2f,
            paint
        )
        paint.color = primaryColor
        canvas.drawText(
            context.getString(R.string.direction_north),
            width / 2f,
            height / 2f - compassSize / 4f + rect.height() / 2f,
            paint
        )
        canvas.restore()

        canvas.save()
        canvas.rotate(180f, width / 2f, height / 2f)
        paint.getTextBounds(south, 0, south.length, rect)
        paint.color = secondaryColor
        canvas.drawRect(
            width / 2f - rect.width() / 2f - 8f,
            height / 2f - compassSize / 4f + rect.height() / 2f,
            width / 2f + rect.width() / 2f + 8f,
            height / 2f - compassSize / 4f - rect.height() / 2f,
            paint
        )
        paint.color = Color.WHITE
        canvas.drawText(
            south,
            width / 2f,
            height / 2f - compassSize / 4f + rect.height() / 2f,
            paint
        )
        canvas.restore()

        canvas.save()
        canvas.rotate(90f, width / 2f, height / 2f)
        paint.getTextBounds(east, 0, east.length, rect)
        paint.color = secondaryColor
        canvas.drawRect(
            width / 2f - rect.width() / 2f - 8f,
            height / 2f - compassSize / 4f + rect.height() / 2f,
            width / 2f + rect.width() / 2f + 8f,
            height / 2f - compassSize / 4f - rect.height() / 2f,
            paint
        )
        paint.color = Color.WHITE
        canvas.drawText(
            east,
            width / 2f,
            height / 2f - compassSize / 4f + rect.height() / 2f,
            paint
        )
        canvas.restore()

        canvas.save()
        canvas.rotate(270f, width / 2f, height / 2f)
        paint.getTextBounds(west, 0, west.length, rect)
        paint.color = secondaryColor
        canvas.drawRect(
            width / 2f - rect.width() / 2f - 8f,
            height / 2f - compassSize / 4f + rect.height() / 2f,
            width / 2f + rect.width() / 2f + 8f,
            height / 2f - compassSize / 4f - rect.height() / 2f,
            paint
        )
        paint.color = Color.WHITE
        canvas.drawText(
            west,
            width / 2f,
            height / 2f - compassSize / 4f + rect.height() / 2f,
            paint
        )
        canvas.restore()
    }

    private fun drawBearings(canvas: Canvas) {
        val maxDistanceMeters = prefs.navigation.maxBeaconDistance
        for (indicator in indicators) {
            paint.colorFilter = if (indicator.tint != null) {
                PorterDuffColorFilter(indicator.tint, PorterDuff.Mode.SRC_IN)
            } else {
                null
            }
            paint.alpha = (255 * indicator.opacity).toInt()
            canvas.save()
            canvas.rotate(indicator.bearing.value, width / 2f, height / 2f)
            val bitmap = getBitmap(indicator.icon)

            val top = if (indicator.distance == null || maxDistanceMeters == 0f) {
                0f
            } else {
                val pctDist = indicator.distance.meters().distance / maxDistanceMeters

                if (pctDist > 1) {
                    0f
                } else {
                    height / 2f - pctDist * compassSize / 2f
                }
            }

            if (top == 0f) {
                canvas.drawBitmap(
                    bitmap,
                    width / 2f - iconSize / 2f,
                    top,
                    paint
                )
            } else {
                paint.color = primaryColor
                canvas.drawCircle(width / 2f, top, radarSize / 2f, paint)
            }
            canvas.restore()
        }
        paint.colorFilter = null
        paint.alpha = 255
    }

    private fun getBitmap(@DrawableRes id: Int, size: Int = iconSize): Bitmap {
        val bitmap = if (icons.containsKey(id)) {
            icons[id]
        } else {
            val drawable = UiUtils.drawable(context, id)
            val bm = drawable?.toBitmap(size, size)
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

    private fun sp(size: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, size,
            resources.displayMetrics
        )
    }


}