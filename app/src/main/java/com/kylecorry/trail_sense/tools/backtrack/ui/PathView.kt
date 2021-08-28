package com.kylecorry.trail_sense.tools.backtrack.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.core.math.cosDegrees
import com.kylecorry.andromeda.core.math.power
import com.kylecorry.andromeda.core.math.sinDegrees
import com.kylecorry.andromeda.core.math.wrap
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.andromeda.core.units.Distance
import com.kylecorry.andromeda.core.units.DistanceUnits
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.paths.GrayPathLineDrawerDecoratorStrategy
import com.kylecorry.trail_sense.shared.paths.PathLineDrawerFactory
import com.kylecorry.trail_sense.shared.toPixelLines
import com.kylecorry.trail_sense.tools.backtrack.domain.waypointcolors.DefaultPointColoringStrategy
import com.kylecorry.trail_sense.tools.backtrack.domain.waypointcolors.IPointColoringStrategy
import com.kylecorry.trailsensecore.domain.geo.GeoService
import com.kylecorry.trailsensecore.domain.geo.PathPoint
import com.kylecorry.trailsensecore.domain.geo.PathStyle
import com.kylecorry.trailsensecore.domain.pixels.PixelCircle
import com.kylecorry.trailsensecore.domain.pixels.PixelLine
import com.kylecorry.trailsensecore.domain.pixels.PixelLineStyle
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.min

class PathView(context: Context, attrs: AttributeSet? = null) : CanvasView(context, attrs) {

    var pointColoringStrategy: IPointColoringStrategy =
        DefaultPointColoringStrategy(Color.TRANSPARENT)
        set(value) {
            field = value
            invalidate()
        }

    var path: List<PathPoint> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

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

    var arePointsHighlighted: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private var pointClickListener: (point: PathPoint) -> Unit = {}
    private var pathCircles: List<Pair<PathPoint, PixelCircle>> = listOf()

    private val prefs by lazy { UserPreferences(context) }
    private val formatService by lazy { FormatService(context) }
    private val pathColor by lazy { prefs.navigation.backtrackPathColor }
    private val pathStyle by lazy { prefs.navigation.backtrackPathStyle }
    private val geoService = GeoService()
    private var metersPerPixel: Float = 1f
    private var center: Coordinate = Coordinate.zero

    init {
        runEveryCycle = false
    }

    override fun setup() {
    }

    override fun draw() {
        clear()
        drawMap()
    }

    fun setOnPointClickListener(listener: (point: PathPoint) -> Unit) {
        pointClickListener = listener
    }

    private fun drawMap() {
        val bounds = geoService.getBounds(path.map { it.coordinate })

        val distanceX = bounds.width().meters().distance
        val distanceY = bounds.height().meters().distance

        if (distanceX == 0f || distanceY == 0f) {
            return
        }

        val h = height.toFloat() - dp(32f)
        val w = width.toFloat() - dp(32f)
        val scale = scaleToFit(distanceX, distanceY, w, h)
        metersPerPixel = 1 / scale
        center = bounds.center

        val gridGap = getGridSize(Distance.meters(distanceX))
        drawGrid(metersPerPixel, gridGap.meters().distance)
        drawLegend(gridGap)

        val pathLines =
            path.map { it.coordinate }.toPixelLines(pathColor.color, mapPixelLineStyle(pathStyle)) {
                getPixels(it)
            }
        drawPaths(pathLines)
        drawWaypoints(path)
        location?.let {
            drawLocation(getPixels(it))
        }
    }

    // TODO: Extract this
    private fun scaleToFit(
        width: Float,
        height: Float,
        maxWidth: Float,
        maxHeight: Float
    ): Float {
        return min(maxWidth / width, maxHeight / height)
    }

    private fun getGridSize(distance: Distance): Distance {
        val baseUnits = prefs.baseDistanceUnits

        val d = distance.meters().distance

        if (d == 0f) {
            return Distance(1f, baseUnits)
        }

        val exponent = (floor(log10(d / 5f))).coerceAtLeast(1f).toInt()

        return if (baseUnits == DistanceUnits.Meters) {
            Distance.meters(power(10, exponent).toFloat())
        } else {
            Distance.feet(power(10, exponent) * 3f)
        }
    }

    private fun drawWaypoints(points: List<PathPoint>) {
        val pointDiameter = dp(5f)
        noPathEffect()
        noStroke()
        val circles = mutableListOf<Pair<PathPoint, PixelCircle>>()
        for (point in points) {
            val color = pointColoringStrategy.getColor(point)
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

    private fun drawLegend(gridGap: Distance) {
        textMode(TextMode.Corner)
        textSize(sp(14f))
        strokeWeight(0f)
        fill(Color.WHITE)
        val distanceText = context.getString(
            R.string.grid_size,
            formatService.formatDistance(gridGap)
        )
        val textWidth = textWidth(distanceText)
        text(distanceText, width - textWidth - dp(16f), height.toFloat() - dp(16f))
    }

    // TODO: Use Andromeda CanvasView.grid method
    fun CanvasView.grid(
        spacing: Float,
        width: Float = this.width.toFloat(),
        height: Float = this.height.toFloat()
    ) {
        // Vertical
        var i = 0f
        while (i < width) {
            line(i, 0f, i, height)
            i += spacing
        }

        // Horizontal
        i = 0f
        while (i < height) {
            line(0f, i, width, i)
            i += spacing
        }
    }

    private fun drawGrid(
        metersPerPixel: Float,
        gap: Float
    ) {
        noFill()
        stroke(Color.WHITE)
        strokeWeight(dp(0.5f))
        opacity(50)
        grid(gap / metersPerPixel)
        opacity(255)
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

    private fun drawPaths(pathLines: List<PixelLine>) {

        val lineDrawerFactory = PathLineDrawerFactory()

        clear()
        for (line in pathLines) {
            val drawer = if (arePointsHighlighted) {
                GrayPathLineDrawerDecoratorStrategy(lineDrawerFactory.create(line.style))
            } else {
                lineDrawerFactory.create(line.style)
            }

            drawer.draw(this, line)
        }
        opacity(255)
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

    private fun mapPixelLineStyle(style: PathStyle): PixelLineStyle {
        return when (style) {
            PathStyle.Solid -> PixelLineStyle.Solid
            PathStyle.Dotted -> PixelLineStyle.Dotted
            PathStyle.Arrow -> PixelLineStyle.Arrow
        }
    }

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {

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

    private val gestureDetector = GestureDetector(context, mGestureListener)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        invalidate()
        return true
    }
}