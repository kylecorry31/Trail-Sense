package com.kylecorry.trail_sense.navigation.paths.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Path
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.cosDegrees
import com.kylecorry.sol.math.SolMath.sinDegrees
import com.kylecorry.sol.math.SolMath.wrap
import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.LineStyle
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.navigation.paths.domain.waypointcolors.IPointColoringStrategy
import com.kylecorry.trail_sense.navigation.paths.domain.waypointcolors.NoDrawPointColoringStrategy
import com.kylecorry.trail_sense.navigation.paths.ui.drawing.PathLineDrawerFactory
import com.kylecorry.trail_sense.navigation.paths.ui.drawing.RenderedPathFactory
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.canvas.PixelCircle
import kotlin.math.max


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

    var isInteractive = false

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
    private val scaleBar = Path()
    private val distanceScale = DistanceScale()

    private val lookupMatrix = Matrix()

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

        clear()
        drawPaths()
        drawWaypoints(path)
        location?.let {
            drawLocation(getPixels(it))
        }
    }

    private fun drawWaypoints(points: List<PathPoint>) {
        val scale = max(scale, 1f)
        val pointDiameter = dp(5f) / scale
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
        val scale = max(scale, 1f)
        stroke(Color.WHITE)
        strokeWeight(dp(1f) / scale)
        fill(Resources.color(context, R.color.brand_orange))
        push()
        rotate(azimuth, pixels.x, pixels.y)
        triangle(
            pixels.x, pixels.y - dp(6f) / scale,
            pixels.x - dp(5f) / scale, pixels.y + dp(6f) / scale,
            pixels.x + dp(5f) / scale, pixels.y + dp(6f) / scale
        )
        pop()
    }

    private fun drawPaths() {
        if (!pathInitialized) {
            drawnPath.reset()
            val factory = RenderedPathFactory(metersPerPixel, null, 0f, true)
            factory.render(this.path.map {it.coordinate}, drawnPath)
            pathInitialized = true
        }

        val lineDrawerFactory = PathLineDrawerFactory()
        val drawer = lineDrawerFactory.create(pathStyle)
        push()
        val offset = getPixels(center)
        translate(offset.x, offset.y)
        drawer.draw(this, pathColor, scale) {
            path(drawnPath)
        }
        noStroke()
        fill(Color.WHITE)
        noPathEffect()
        pop()
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
        strokeWeight(4f)

        val metersPerPixel = metersPerPixel / scale
        val scaleSize = distanceScale.getScaleDistance(units, width / 2f, metersPerPixel)

        scaleBar.reset()
        distanceScale.getScaleBar(scaleSize, metersPerPixel, scaleBar)
        val start = width - dp(16f) - pathWidth(scaleBar)
        val y = height - dp(16f)
        push()
        translate(start, y)
        path(scaleBar)
        pop()

        textMode(TextMode.Corner)
        textSize(sp(12f))
        noStroke()
        fill(Color.WHITE)
        val scaleText =
            formatService.formatDistance(scaleSize, Units.getDecimalPlaces(scaleSize.units), false)
        text(
            scaleText,
            start - textWidth(scaleText) - dp(4f),
            y + textHeight(scaleText) / 2
        )
    }

    fun recenter() {
        translateX = 0f
        translateY = 0f
        scale = 1f
    }

    private fun zoom(factor: Float) {
        val newScale = (scale * factor).coerceIn(0.25f, 16f)
        val newFactor = newScale / scale
        scale *= newFactor
        translateX *= newFactor
        translateY *= newFactor
    }

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            // TODO: Keep path on screen
            translateX -= distanceX
            translateY -= distanceY
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            // TODO: Center place tapped before zooming
            zoom(2F)
            return super.onDoubleTap(e)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val pixel = viewToSourceCoord(PixelCoordinate(e.x, e.y))

            val tapRadius = dp(12f)
            val closest = pathCircles.minByOrNull { it.second.center.distanceTo(pixel) }

            if (closest != null && closest.second.center.distanceTo(pixel) < tapRadius) {
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
        if (isInteractive) {
            mScaleDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            invalidate()
        }
        return true
    }

    private fun viewToSourceCoord(screen: PixelCoordinate): PixelCoordinate {
        lookupMatrix.reset()
        val point = floatArrayOf(screen.x, screen.y)
        lookupMatrix.postScale(scale, scale, width / 2f, height / 2f)
        lookupMatrix.postTranslate(translateX, translateY)
        lookupMatrix.invert(lookupMatrix)
        lookupMatrix.mapPoints(point)
        return PixelCoordinate(point[0], point[1])
    }

}