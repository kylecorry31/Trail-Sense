package com.kylecorry.trail_sense.navigation.paths.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.cosDegrees
import com.kylecorry.sol.math.SolMath.sinDegrees
import com.kylecorry.sol.math.SolMath.wrap
import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.LineStyle
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.navigation.paths.domain.waypointcolors.IPointColoringStrategy
import com.kylecorry.trail_sense.navigation.paths.domain.waypointcolors.NoDrawPointColoringStrategy
import com.kylecorry.trail_sense.navigation.paths.ui.drawing.PathLineDrawerFactory
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.canvas.PixelCircle
import com.kylecorry.trail_sense.shared.toCanvasPath


class PathView(context: Context, attrs: AttributeSet? = null) : CanvasView(context, attrs) {

    // TODO: Update this to use a mappable path
    var pointColoringStrategy: IPointColoringStrategy = NoDrawPointColoringStrategy()
        set(value) {
            field = value
            invalidate()
        }

    var path: List<PathPoint> = emptyList()
        set(value) {
            field = value
            pathInitialized = false
            invalidate()
        }

    private var pathInitialized = false
    private var drawnPath = Path()

    var location: Coordinate? = null
        set(value) {
            field = value
            invalidate()
        }

    var azimuth: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    private var pointClickListener: (point: PathPoint) -> Unit = {}
    private var pathCircles: List<Pair<PathPoint, PixelCircle>> = listOf()

    var pathColor = Color.BLACK
    var pathStyle = LineStyle.Dotted
    private val geoService = GeologyService()
    private var metersPerPixel: Float = 1f
    private var center: Coordinate = Coordinate.zero

    private var translateX = 0f
    private var translateY = 0f
    private var scale = 1f
    private var distanceX = 0f

    private val prefs by lazy { UserPreferences(context) }
    private val units by lazy { prefs.baseDistanceUnits }
    private val formatService by lazy { FormatService(context) }

    init {
        runEveryCycle = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        pathInitialized = false
    }

    override fun setup() {
        recenter()
    }

    override fun draw() {
        clear()
        push()
        drawScale()
        translate(translateX, translateY)
        scale(scale, scale, width / 2f, height / 2f)
        drawMap()
        pop()
    }

    fun setOnPointClickListener(listener: (point: PathPoint) -> Unit) {
        pointClickListener = listener
    }

    private fun drawMap() {
        val bounds = geoService.getBounds(path.map { it.coordinate })

        distanceX = bounds.width().meters().distance
        val distanceY = bounds.height().meters().distance

        if (distanceX == 0f || distanceY == 0f) {
            return
        }

        val h = height.toFloat() - dp(32f)
        val w = width.toFloat() - dp(32f)
        val scale = SolMath.scaleToFit(distanceX, distanceY, w, h)
        metersPerPixel = 1 / scale
        center = bounds.center

        drawPaths()
        drawWaypoints(path)
        location?.let {
            drawLocation(getPixels(it))
        }
    }

    private fun drawWaypoints(points: List<PathPoint>) {
        val pointDiameter = dp(5f)
        noPathEffect()
        noStroke()
        val circles = mutableListOf<Pair<PathPoint, PixelCircle>>()
        for (point in points) {
            val color = pointColoringStrategy.getColor(point) ?: continue
            if (color == Color.TRANSPARENT) {
                continue
            }
            fill(color)
            val position = getPixels(point.coordinate)
            circle(position.x, position.y, pointDiameter)
            circles.add(
                point to PixelCircle(
                    PixelCoordinate(position.x, position.y),
                    pointDiameter
                )
            )
        }

        pathCircles = circles
    }

    private fun drawLocation(pixels: PixelCoordinate) {
        stroke(Color.WHITE)
        strokeWeight(dp(1f))
        fill(Resources.color(context, R.color.colorPrimary))
        push()
        rotate(azimuth, pixels.x, pixels.y)
        triangle(
            pixels.x, pixels.y - dp(6f),
            pixels.x - dp(5f), pixels.y + dp(6f),
            pixels.x + dp(5f), pixels.y + dp(6f)
        )
        pop()
    }

    private fun drawPaths() {
        if (!pathInitialized) {
            drawnPath.reset()
            this.path.map { it.coordinate }.toCanvasPath(drawnPath) { getPixels(it) }
            pathInitialized = true
        }

        val lineDrawerFactory = PathLineDrawerFactory()
        val drawer = lineDrawerFactory.create(pathStyle)
        clear()
        drawer.draw(this, pathColor, scale) {
            path(drawnPath)
        }
        noStroke()
        fill(Color.WHITE)
        noPathEffect()
    }

    private fun getPixels(
        location: Coordinate
    ): PixelCoordinate {
        val distance = center.distanceTo(location)
        val bearing = center.bearingTo(location)
        val angle = wrap(-(bearing.value - 90), 0f, 360f)
        val pixelDistance = distance / metersPerPixel
        val xDiff = cosDegrees(angle.toDouble()).toFloat() * pixelDistance
        val yDiff = sinDegrees(angle.toDouble()).toFloat() * pixelDistance
        return PixelCoordinate(width / 2f + xDiff, height / 2f - yDiff)
    }

    private fun drawScale() {
        noFill()
        stroke(Color.WHITE)

        val strokeSize = 4f

        strokeWeight(strokeSize)

        val scaleSize = getScaleSize(width / 2f)

        val length = scale * scaleSize.meters().distance / metersPerPixel

        val start = width - dp(16f) - length
        val end = start + length
        val y = height.toFloat() - dp(16f)

        val offset = 14

        line(start - strokeSize / 2, y - offset, start - strokeSize / 2, y + offset)
        line(end + strokeSize / 2, y - offset, end + strokeSize / 2, y + offset)
        line((start + end) / 2, y + offset, (start + end) / 2, y)
        line(start, y, end, y)

        textMode(TextMode.Corner)
        textSize(sp(12f))
        noStroke()
        fill(Color.WHITE)
        val scaleText =
            formatService.formatDistance(scaleSize, Units.getDecimalPlaces(scaleSize.units), false)
        text(
            scaleText,
            start - textWidth(scaleText) - dp(4f) - strokeSize,
            y + textHeight(scaleText) / 2
        )
    }

    private fun getScaleSize(maxLength: Float): Distance {
        val intervals = if (units == DistanceUnits.Meters) {
            metricScaleIntervals
        } else {
            imperialScaleIntervals
        }

        for (i in 1..intervals.lastIndex) {
            val current = intervals[i]
            val length = scale * current.meters().distance / metersPerPixel
            if (length > maxLength) {
                return intervals[i - 1]
            }
        }

        return intervals.last()
    }

    fun recenter() {
        translateX = 0f
        translateY = 0f
        scale = 1f
    }

    private fun zoom(factor: Float) {
        scale *= factor
        translateX *= factor
        translateY *= factor
    }

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            translateX -= distanceX
            translateY -= distanceY
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            // TODO: Zoom to the place tapped
            zoom(2F)
            return super.onDoubleTap(e)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val screenCoord = PixelCoordinate(e.x, e.y)

            val tapRadius = dp(12f)
            val closest = pathCircles.minByOrNull { it.second.center.distanceTo(screenCoord) }

            if (closest != null && closest.second.center.distanceTo(screenCoord) < tapRadius) {
                pointClickListener.invoke(closest.first)
            }

            return super.onSingleTapConfirmed(e)
        }
    }

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            zoom(detector.scaleFactor)
            return true
        }
    }

    private val gestureDetector = GestureDetector(context, mGestureListener)
    private val mScaleDetector = ScaleGestureDetector(context, scaleListener)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mScaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        invalidate()
        return true
    }

    companion object {
        private val metricScaleIntervals = listOf(
            Distance.meters(1f),
            Distance.meters(2f),
            Distance.meters(5f),
            Distance.meters(10f),
            Distance.meters(20f),
            Distance.meters(50f),
            Distance.meters(100f),
            Distance.meters(200f),
            Distance.meters(500f),
            Distance.kilometers(1f),
            Distance.kilometers(2f),
            Distance.kilometers(5f),
            Distance.kilometers(10f),
            Distance.kilometers(20f),
            Distance.kilometers(50f),
            Distance.kilometers(100f),
            Distance.kilometers(200f),
            Distance.kilometers(500f),
            Distance.kilometers(1000f),
            Distance.kilometers(2000f),
        )

        private val imperialScaleIntervals = listOf(
            Distance.feet(10f),
            Distance.feet(20f),
            Distance.feet(50f),
            Distance.feet(100f),
            Distance.feet(200f),
            Distance.feet(500f),
            Distance.miles(0.25f),
            Distance.miles(0.5f),
            Distance.miles(1f),
            Distance.miles(2f),
            Distance.miles(5f),
            Distance.miles(10f),
            Distance.miles(20f),
            Distance.miles(50f),
            Distance.miles(100f),
            Distance.miles(200f),
            Distance.miles(500f),
            Distance.miles(1000f),
            Distance.miles(2000f),
        )
    }
}