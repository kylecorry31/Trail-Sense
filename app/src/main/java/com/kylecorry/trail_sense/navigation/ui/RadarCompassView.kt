package com.kylecorry.trail_sense.navigation.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.toBitmap
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trailsensecore.domain.geo.Bearing
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.geo.Path
import com.kylecorry.trailsensecore.domain.math.cosDegrees
import com.kylecorry.trailsensecore.domain.math.deltaAngle
import com.kylecorry.trailsensecore.domain.math.sinDegrees
import com.kylecorry.trailsensecore.domain.math.wrap
import com.kylecorry.trailsensecore.domain.pixels.PixelCoordinate
import com.kylecorry.trailsensecore.domain.pixels.PixelLine
import com.kylecorry.trailsensecore.domain.pixels.PixelLineStyle
import com.kylecorry.trailsensecore.domain.pixels.toPixelLines
import com.kylecorry.trailsensecore.domain.units.Distance
import com.kylecorry.trailsensecore.domain.units.IsLargeUnitSpecification
import com.kylecorry.trailsensecore.infrastructure.canvas.ArrowPathEffect
import com.kylecorry.trailsensecore.infrastructure.canvas.DottedPathEffect
import com.kylecorry.trailsensecore.infrastructure.canvas.getMaskedBitmap
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import kotlin.math.min

class RadarCompassView : View, ICompassView {
    private lateinit var paint: Paint
    private lateinit var center: PixelCoordinate
    private val icons = mutableMapOf<Int, Bitmap>()
    private var indicators = listOf<BearingIndicator>()
    private var compass: Bitmap? = null
    private var isInit = false
    private var azimuth = 0f
    private var destination: Float? = null

    @ColorInt
    private var destinationColor: Int? = null

    private val prefs by lazy { UserPreferences(context) }

    @ColorInt
    private var primaryColor: Int = Color.WHITE

    @ColorInt
    private var secondaryColor: Int = Color.WHITE

    @ColorInt
    private var blue: Int = Color.BLUE
    private val formatService by lazy { FormatServiceV2(context) }

    private var iconSize = 0
    private var radarSize = 0
    private var directionSize = 0
    private var compassSize = 0
    private var distanceSize = 0f
    private var cardinalSize = 0f
    private val rect = Rect()

    private lateinit var compassMask: Bitmap
    private lateinit var pathBitmap: Bitmap

    private var metersPerPixel = 1f
    private var location = Coordinate.zero
    private var useTrueNorth = false
    private var declination: Float = 0f
    private var pathLines = listOf<PixelLine>()

    private lateinit var maxDistanceBaseUnits: Distance
    private lateinit var maxDistanceMeters: Distance

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
        if (visibility != VISIBLE) {
            return
        }
        if (!isInit) {
            paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.textAlign = Paint.Align.CENTER
            iconSize = UiUtils.dp(context, 24f).toInt()
            radarSize = UiUtils.dp(context, 10f).toInt()
            directionSize = UiUtils.dp(context, 16f).toInt()
            compassSize = min(height, width) - 2 * iconSize - 2 * UiUtils.dp(context, 2f).toInt()
            isInit = true
            distanceSize = UiUtils.sp(context, 8f)
            cardinalSize = UiUtils.sp(context, 10f)
            primaryColor = UiUtils.color(context, R.color.colorPrimary)
            secondaryColor = UiUtils.color(context, R.color.colorSecondary)
            blue = UiUtils.color(context, R.color.colorAccent)
            val compassDrawable = UiUtils.drawable(context, R.drawable.radar)
            compass = compassDrawable?.toBitmap(compassSize, compassSize)
            useTrueNorth = prefs.navigation.useTrueNorth
            maxDistanceMeters = Distance.meters(prefs.navigation.maxBeaconDistance)
            maxDistanceBaseUnits = maxDistanceMeters.convertTo(prefs.baseDistanceUnits)
            metersPerPixel = maxDistanceMeters.distance / (compassSize / 2f)
            north = context.getString(R.string.direction_north)
            south = context.getString(R.string.direction_south)
            east = context.getString(R.string.direction_east)
            west = context.getString(R.string.direction_west)
            compassMask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val tempCanvas = Canvas(compassMask)
            paint.color = Color.WHITE
            paint.style = Paint.Style.FILL
            tempCanvas.drawCircle(width / 2f, height / 2f, compassSize / 2f, paint)
            pathBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            center = PixelCoordinate(width / 2f, height / 2f)
        }
        canvas.drawColor(Color.TRANSPARENT)
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
        val dp2 = UiUtils.dp(context, 2f)
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

    private fun drawPaths(canvas: Canvas) {
        // TODO: Improve the performance of this
        val pathBitmap = canvas.getMaskedBitmap(compassMask, pathBitmap) {
            val dotted = DottedPathEffect()
            val arrow = ArrowPathEffect()
            it.drawColor(Color.TRANSPARENT)
            for (line in pathLines) {

                if (!shouldDisplayLine(line)){
                    continue
                }

                when (line.style){
                    PixelLineStyle.Solid -> {
                        paint.pathEffect = null
                        paint.style = Paint.Style.STROKE
                        paint.strokeCap = Paint.Cap.ROUND
                        paint.strokeWidth = 6f
                    }
                    PixelLineStyle.Arrow -> {
                        paint.pathEffect = arrow
                        paint.style = Paint.Style.FILL
                    }
                    PixelLineStyle.Dotted -> {
                        paint.pathEffect = dotted
                        paint.style = Paint.Style.FILL
                    }
                }
                paint.color = line.color
                paint.alpha = line.alpha
                it.drawLine(line.start.x, line.start.y, line.end.x, line.end.y, paint)
            }
            paint.alpha = 255
            paint.strokeCap = Paint.Cap.BUTT
            paint.style = Paint.Style.FILL
            paint.pathEffect = null
        }

        canvas.drawBitmap(pathBitmap, 0f, 0f, paint)
    }

    private fun shouldDisplayLine(line: PixelLine): Boolean {
        if (line.alpha == 0){
            return false
        }

        if (getDistanceFromCenter(line.start) > compassSize / 2 && getDistanceFromCenter(line.end) > compassSize / 2){
            return false
        }

        return true
    }


    private fun getDistanceFromCenter(pixel: PixelCoordinate): Float {
        return pixel.distanceTo(center)
    }

    fun finalize() {
        try {
            compassMask.recycle()
            pathBitmap.recycle()
            compass?.recycle()
        } catch (e: Exception) {
        }
    }

    override fun setAzimuth(bearing: Float) {
        azimuth = bearing
        invalidate()
    }

    override fun setLocation(location: Coordinate) {
        this.location = location
        invalidate()
    }

    override fun setDeclination(declination: Float) {
        this.declination = declination
        invalidate()
    }

    override fun setIndicators(indicators: List<BearingIndicator>) {
        this.indicators = indicators
        invalidate()
    }

    fun setPaths(paths: List<Path>) {
        val maxTimeAgo = prefs.navigation.showBacktrackPathDuration
        pathLines = paths.flatMap {
            it.toPixelLines(maxTimeAgo) {
                coordinateToPixel(it)
            }
        }
        invalidate()
    }

    override fun setDestination(bearing: Float?, @ColorInt color: Int?) {
        destination = bearing
        destinationColor = color
        invalidate()
    }

    private fun drawCompass(canvas: Canvas) {
        paint.alpha = 255
        canvas.drawBitmap(
            compass!!,
            iconSize.toFloat() + UiUtils.dp(context, 2f),
            iconSize.toFloat() + UiUtils.dp(context, 2f),
            paint
        )
        drawPaths(canvas)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.save()
        canvas.rotate(azimuth, width / 2f, height / 2f)
        if (destination == null) {
            paint.color = gray(60)
            canvas.drawLine(
                width / 2f,
                height / 2f,
                width / 2f,
                iconSize.toFloat() + UiUtils.dp(context, 2f),
                paint
            )
        }
        canvas.drawBitmap(
            getBitmap(R.drawable.ic_beacon, directionSize), width / 2f - directionSize / 2f,
            height / 2f - directionSize / 2f,
            paint
        )
        paint.color = Color.rgb(100, 100, 100)
        canvas.drawCircle(width / 2f, height / 2f, compassSize / 4f, paint)
        canvas.drawCircle(width / 2f, height / 2f, 3 * compassSize / 8f, paint)
        canvas.drawCircle(width / 2f, height / 2f, compassSize / 8f, paint)
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        val quarterDist = maxDistanceBaseUnits.times(0.25f).toRelativeDistance()
        val threeQuarterDist = maxDistanceBaseUnits.times(0.75f).toRelativeDistance()
        paint.textSize = distanceSize

        // TODO: This doesn't need to happen on every draw
        val quarterText = formatService.formatDistance(
            quarterDist,
            if (IsLargeUnitSpecification().isSatisfiedBy(quarterDist.units)) 1 else 0
        )
        val threeQuarterText = formatService.formatDistance(
            threeQuarterDist,
            if (IsLargeUnitSpecification().isSatisfiedBy(threeQuarterDist.units)) 1 else 0
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

            val top = if (indicator.distance == null || maxDistanceMeters.distance == 0f) {
                0f
            } else {
                val pctDist = indicator.distance.meters().distance / maxDistanceMeters.distance

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
                paint.color = Color.WHITE
                canvas.drawCircle(
                    width / 2f,
                    top,
                    radarSize / 2f + UiUtils.dp(context, 0.5f),
                    paint
                )
                paint.color = primaryColor
                canvas.drawCircle(width / 2f, top, radarSize / 2f, paint)
            }
            canvas.restore()
        }
        paint.colorFilter = null
        paint.alpha = 255
    }

    private fun coordinateToPixel(coordinate: Coordinate): PixelCoordinate {
        val distance = location.distanceTo(coordinate)
        val bearing =
            location.bearingTo(coordinate).withDeclination(if (useTrueNorth) 0f else -declination)
        val angle = wrap(-(bearing.value - 90), 0f, 360f)
        val pixelDistance = distance / metersPerPixel
        val xDiff = cosDegrees(angle.toDouble()).toFloat() * pixelDistance
        val yDiff = sinDegrees(angle.toDouble()).toFloat() * pixelDistance
        return PixelCoordinate(width / 2f + xDiff, height / 2f - yDiff)
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
}