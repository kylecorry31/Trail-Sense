package com.kylecorry.trail_sense.navigation.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.views.CanvasView
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
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import kotlin.math.min

class RadarCompassView : CanvasView, ICompassView {
    private lateinit var center: PixelCoordinate
    private val icons = mutableMapOf<Int, Bitmap>()
    private var indicators = listOf<BearingIndicator>()
    private var compass: Bitmap? = null
    private var pathBitmap: Bitmap? = null
    private var azimuth = 0f
    private var destination: Float? = null

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

    init {
        runEveryCycle = false
        setupAfterVisible = true
    }

    private fun drawDestination() {
        destination ?: return
        val color = destinationColor ?: primaryColor
        push()
        fill(color)
        opacity(100)
        val dp2 = dp(2f)
        arc(
            iconSize.toFloat() + dp2,
            iconSize.toFloat() + dp2,
            compassSize.toFloat(),
            compassSize.toFloat(),
            azimuth - 90,
            azimuth - 90 + deltaAngle(azimuth, destination!!),
            ArcMode.Pie
        )
        opacity(255)
        pop()
    }

    private fun drawPaths() {
        val pathBitmap = mask(compass!!, pathBitmap!!){
            val dotted = DottedPathEffect()
            val arrow = ArrowPathEffect()
            clear()
            for (line in pathLines) {

                if (!shouldDisplayLine(line)) {
                    continue
                }

                when (line.style) {
                    PixelLineStyle.Solid -> {
                        noPathEffect()
                        noFill()
                        stroke(line.color)
                        strokeWeight(6f)
                    }
                    PixelLineStyle.Arrow -> {
                        pathEffect(arrow)
                        noStroke()
                        fill(line.color)
                    }
                    PixelLineStyle.Dotted -> {
                        pathEffect(dotted)
                        noStroke()
                        fill(line.color)
                    }
                }
                opacity(line.alpha)
                val xOffset = (width - compassSize) / 2f
                val yOffset = (height - compassSize) / 2f
                line(line.start.x - xOffset, line.start.y - yOffset, line.end.x - xOffset, line.end.y - yOffset)
                opacity(255)
                noStroke()
                fill(Color.WHITE)
                noPathEffect()
            }
        }

        imageMode(ImageMode.Center)
        image(pathBitmap, width / 2f, height / 2f)
    }

    private fun shouldDisplayLine(line: PixelLine): Boolean {
        if (line.alpha == 0) {
            return false
        }

        if (getDistanceFromCenter(line.start) > compassSize / 2 && getDistanceFromCenter(line.end) > compassSize / 2) {
            return false
        }

        return true
    }


    private fun getDistanceFromCenter(pixel: PixelCoordinate): Float {
        return pixel.distanceTo(center)
    }

    fun finalize() {
        try {
            compass?.recycle()
            pathBitmap?.recycle()
            for (icon in icons){
                icon.value.recycle()
            }
            icons.clear()
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

    private fun drawCompass() {
        imageMode(ImageMode.Center)
        opacity(255)
        image(
            compass!!,
            width / 2f,
            height / 2f,
        )

        drawPaths()

        noFill()
        stroke(color(60))
        strokeWeight(3f)
        push()
        rotate(azimuth)
        if (destination == null) {
            line(width / 2f, height / 2f, width / 2f, iconSize + dp(2f))
        }
        image(getBitmap(R.drawable.ic_beacon, directionSize), width / 2f, height / 2f)
        stroke(color(100))
        circle(width / 2f, height / 2f, compassSize / 2f)
        circle(width / 2f, height / 2f, 3 * compassSize / 4f)
        circle(width / 2f, height / 2f, compassSize / 4f)

        fill(Color.WHITE)
        stroke(UiUtils.color(context, R.color.colorSecondary))

        val quarterDist = maxDistanceBaseUnits.times(0.25f).toRelativeDistance()
        val threeQuarterDist = maxDistanceBaseUnits.times(0.75f).toRelativeDistance()

        textSize(distanceSize)

        // TODO: This doesn't need to happen on every draw
        val quarterText = formatService.formatDistance(
            quarterDist,
            if (IsLargeUnitSpecification().isSatisfiedBy(quarterDist.units)) 1 else 0
        )
        val threeQuarterText = formatService.formatDistance(
            threeQuarterDist,
            if (IsLargeUnitSpecification().isSatisfiedBy(threeQuarterDist.units)) 1 else 0
        )

        textMode(TextMode.Center)
        text(quarterText, width / 2f + compassSize / 8f, height / 2f)
        text(threeQuarterText, width / 2f + 3 * compassSize / 8f, height / 2f)

        pop()

        textSize(cardinalSize)
        push()
        rotate(0f)
        fill(primaryColor)
        text(
            north,
            width / 2f,
            height / 2f - compassSize / 4f
        )
        pop()

        push()
        rotate(180f)
        fill(Color.WHITE)
        text(
            south,
            width / 2f,
            height / 2f - compassSize / 4f
        )
        pop()

        push()
        rotate(90f)
        fill(Color.WHITE)
        text(
            east,
            width / 2f,
            height / 2f - compassSize / 4f
        )
        pop()

        push()
        rotate(270f)
        fill(Color.WHITE)
        text(
            west,
            width / 2f,
            height / 2f - compassSize / 4f
        )
        pop()

        imageMode(ImageMode.Corner)
    }

    private fun drawBearings() {
        for (indicator in indicators) {
            if (indicator.tint != null) {
                tint(indicator.tint)
            } else {
                noTint()
            }
            opacity((255 * indicator.opacity).toInt())
            push()
            rotate(indicator.bearing)
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
                image(bitmap, width / 2f - iconSize / 2f, top)
            } else {
                noTint()
                stroke(Color.WHITE)
                strokeWeight(dp(0.5f))
                fill(indicator.tint ?: primaryColor)
                // TODO: Verify this is correct
                circle(width / 2f, top, radarSize.toFloat())
            }
            pop()
        }
        noTint()
        opacity(255)
        noStroke()
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
            icons[id] = loadImage(id, size, size)
            icons[id]
        }
        return bitmap!!
    }

    override fun setup() {
        iconSize = dp(24f).toInt()
        radarSize = dp(10f).toInt()
        directionSize = dp(16f).toInt()
        compassSize = min(height, width) - 2 * iconSize - 2 * UiUtils.dp(context, 2f).toInt()
        distanceSize = sp(8f)
        cardinalSize = sp(10f)
        primaryColor = UiUtils.color(context, R.color.colorPrimary)
        secondaryColor = UiUtils.color(context, R.color.colorSecondary)
        compass = loadImage(R.drawable.compass, compassSize, compassSize)
        pathBitmap = Bitmap.createBitmap(compassSize, compassSize, Bitmap.Config.ARGB_8888)
        useTrueNorth = prefs.navigation.useTrueNorth
        maxDistanceMeters = Distance.meters(prefs.navigation.maxBeaconDistance)
        maxDistanceBaseUnits = maxDistanceMeters.convertTo(prefs.baseDistanceUnits)
        metersPerPixel = maxDistanceMeters.distance / (compassSize / 2f)
        north = context.getString(R.string.direction_north)
        south = context.getString(R.string.direction_south)
        east = context.getString(R.string.direction_east)
        west = context.getString(R.string.direction_west)
        center = PixelCoordinate(width / 2f, height / 2f)
    }

    override fun draw() {
        if (!isVisible) {
            return
        }
        clear()
        push()
        rotate(-azimuth)
        drawCompass()
        drawBearings()
        drawDestination()
        pop()
    }

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            prefs.navigation.maxBeaconDistance *= detector.scaleFactor
            maxDistanceMeters = Distance.meters(prefs.navigation.maxBeaconDistance)
            maxDistanceBaseUnits = maxDistanceMeters.convertTo(prefs.baseDistanceUnits)
            metersPerPixel = maxDistanceMeters.distance / (compassSize / 2f)
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector?) {
            super.onScaleEnd(detector)
            // TODO: Signal for the beacons to be rescanned
        }
    }

    private val mScaleDetector = ScaleGestureDetector(context, scaleListener)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (prefs.navigation.scaleRadarCompass) {
            mScaleDetector.onTouchEvent(event)
            invalidate()
        }
        return true
    }
}